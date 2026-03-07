package com.aidetective.backend.analysis.integration;

import com.aidetective.backend.analysis.dto.AsiInvestigationResult;
import com.aidetective.backend.analysis.dto.AsiSignalExtractionResult;
import com.aidetective.backend.analysis.exception.UpstreamAiException;
import com.aidetective.backend.analysis.model.AnalysisRecord;
import com.aidetective.backend.analysis.model.InputType;
import com.aidetective.backend.analysis.service.PromptFactory;
import com.aidetective.backend.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AsiOneClient {

    private static final List<String> CLAIM_ASSESSMENTS = List.of(
        "Supported",
        "Questionable",
        "Contradicted",
        "Insufficient Evidence"
    );
    private static final int RETRY_TOKEN_INCREMENT = 600;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PromptFactory promptFactory;

    public AsiOneClient(
        RestClient asiRestClient,
        ObjectMapper objectMapper,
        AppProperties appProperties,
        PromptFactory promptFactory
    ) {
        this.restClient = asiRestClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.promptFactory = promptFactory;
    }

    public AsiSignalExtractionResult extractSignals(
        String text,
        InputType inputType,
        String sourceUrl,
        String sourceTitle,
        String sourceLabel
    ) {
        var prompt = promptFactory.createSignalExtractionText(text, inputType, sourceUrl, sourceTitle, sourceLabel);
        return toSignalExtraction(execute(prompt, "detective_signal_extraction", signalExtractionSchema()).payload());
    }

    public AsiSignalExtractionResult extractImageSignals(
        String imageBase64,
        String imageMimeType,
        String sourceTitle,
        String sourceLabel
    ) {
        var prompt = promptFactory.createSignalExtractionImage(imageMimeType, imageBase64, sourceTitle, sourceLabel);
        return toSignalExtraction(execute(prompt, "detective_visual_extraction", signalExtractionSchema()).payload());
    }

    public AiAnalysis analyzeInvestigation(
        String normalizedText,
        InputType inputType,
        String sourceUrl,
        String sourceTitle,
        String sourceLabel,
        AsiSignalExtractionResult extraction
    ) {
        var prompt = promptFactory.createVerification(
            normalizedText,
            inputType,
            sourceUrl,
            sourceTitle,
            sourceLabel,
            extraction
        );
        var response = execute(prompt, "detective_report", investigationSchema());
        return new AiAnalysis(toResult(response.payload()), response.modelUsed());
    }

    public FollowUpAnswer followUp(AnalysisRecord record, String question) {
        var prompt = promptFactory.createFollowUp(record, question);
        return toFollowUp(execute(prompt, "detective_follow_up", followUpSchema()).payload());
    }

    private StructuredJsonResponse execute(PromptFactory.PromptBundle prompt, String schemaName, Map<String, Object> schema) {
        return execute(prompt, schemaName, schema, appProperties.getAsi().getMaxTokens(), true);
    }

    private StructuredJsonResponse execute(
        PromptFactory.PromptBundle prompt,
        String schemaName,
        Map<String, Object> schema,
        int maxTokens,
        boolean allowRetry
    ) {
        String apiKey = appProperties.getAsi().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new UpstreamAiException("ASI_ONE_API_KEY is not configured on the backend.");
        }

        var payload = Map.<String, Object>ofEntries(
            Map.entry("model", appProperties.getAsi().getModel()),
            Map.entry("temperature", appProperties.getAsi().getTemperature()),
            Map.entry("max_tokens", maxTokens),
            Map.entry("web_search", appProperties.getAsi().isWebSearch()),
            Map.entry("messages", prompt.messages().stream().map(message -> Map.of(
                "role", message.role(),
                "content", message.content()
            )).toList()),
            Map.entry("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                    "name", schemaName,
                    "strict", true,
                    "schema", schema
                )
            ))
        );

        try {
            String responseBody = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

            return parseStructured(responseBody);
        } catch (InvalidStructuredOutputException exception) {
            if (allowRetry) {
                return execute(
                    appendRetryInstruction(prompt),
                    schemaName,
                    schema,
                    maxTokens + RETRY_TOKEN_INCREMENT,
                    false
                );
            }
            throw new UpstreamAiException(exception.getMessage(), exception);
        } catch (RestClientResponseException exception) {
            throw new UpstreamAiException("ASI-1 request failed with status %s: %s".formatted(
                exception.getStatusCode(),
                exception.getResponseBodyAsString()
            ), exception);
        } catch (JsonProcessingException exception) {
            throw new UpstreamAiException("ASI-1 returned an unreadable response.", exception);
        }
    }

    private StructuredJsonResponse parseStructured(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choiceNode = root.path("choices").path(0);
        JsonNode contentNode = choiceNode.path("message").path("content");
        String finishReason = choiceNode.path("finish_reason").asText("");

        if (contentNode.isMissingNode() || contentNode.isNull()) {
            throw new UpstreamAiException("ASI-1 response did not contain a completion message.");
        }

        String content = extractContent(contentNode);
        if (content.isBlank()) {
            throw new UpstreamAiException("ASI-1 returned an empty completion.");
        }

        String modelUsed = root.path("model").asText(appProperties.getAsi().getModel());
        String normalizedJson = normalizeJsonPayload(content);

        try {
            return new StructuredJsonResponse(objectMapper.readTree(normalizedJson), modelUsed);
        } catch (JsonProcessingException exception) {
            String repairedJson = repairLikelyTruncatedJson(normalizedJson);
            if (!repairedJson.equals(normalizedJson)) {
                try {
                    return new StructuredJsonResponse(objectMapper.readTree(repairedJson), modelUsed);
                } catch (JsonProcessingException ignored) {
                    // fall through to structured output error
                }
            }

            throw new InvalidStructuredOutputException(
                "ASI-1 returned a response that could not be parsed as the expected JSON schema%s. Raw content preview: %s"
                    .formatted(
                        "length".equalsIgnoreCase(finishReason) ? " because it appears to have been truncated" : "",
                        preview(normalizedJson)
                    ),
                exception
            );
        }
    }

    private AsiSignalExtractionResult toSignalExtraction(JsonNode node) {
        List<AsiSignalExtractionResult.AsiClaimCandidate> claims = node.path("claims").isArray()
            ? stream(node.path("claims")).map(claim -> new AsiSignalExtractionResult.AsiClaimCandidate(
                textOrNull(claim, "statement", "claim"),
                textOrNull(claim, "why_it_matters", "whyItMatters", "importance")
            )).toList()
            : List.of();

        List<AsiSignalExtractionResult.AsiEntity> entities = node.path("entities").isArray()
            ? stream(node.path("entities")).map(entity -> new AsiSignalExtractionResult.AsiEntity(
                textOrNull(entity, "name", "nom", "entity"),
                textOrNull(entity, "type", "categorie", "catégorie"),
                textOrNull(entity, "context", "contexte", "details", "détails")
            )).toList()
            : List.of();

        return new AsiSignalExtractionResult(
            textOrNull(node, "summary", "resume", "résumé", "synthese", "synthèse"),
            claims,
            entities,
            textOrNull(node, "content_language", "contentLanguage", "language", "langue"),
            textListOrEmpty(node, "risk_labels", "riskLabels", "risks")
        );
    }

    private AsiInvestigationResult toResult(JsonNode node) {
        List<AsiInvestigationResult.AsiClaim> claims = node.path("claims").isArray()
            ? stream(node.path("claims")).map(claim -> new AsiInvestigationResult.AsiClaim(
                textOrNull(claim, "statement", "declaration", "déclaration", "claim", "affirmation"),
                textOrNull(claim, "suspicion", "suspicion_note", "note_de_suspicion", "red_flag", "alerte"),
                textOrNull(claim, "evidence_hint", "indice_de_preuve", "preuve", "evidence", "justification"),
                textOrNull(claim, "assessment", "claim_assessment", "status"),
                integerOrNull(claim, "confidence", "confidence_score"),
                textListOrEmpty(claim, "evidence_points", "evidencePoints", "evidence_bullets"),
                textListOrEmpty(claim, "source_urls", "sourceUrls", "urls"),
                textOrNull(claim, "next_step", "nextStep", "follow_up")
            )).toList()
            : List.of();

        List<AsiInvestigationResult.AsiEntity> entities = node.path("entities").isArray()
            ? stream(node.path("entities")).map(entity -> new AsiInvestigationResult.AsiEntity(
                textOrNull(entity, "name", "nom", "entity"),
                textOrNull(entity, "type", "categorie", "catégorie"),
                textOrNull(entity, "context", "contexte", "details", "détails")
            )).toList()
            : List.of();

        return new AsiInvestigationResult(
            textOrNull(node, "summary", "resume", "résumé", "synthese", "synthèse"),
            claims,
            entities,
            textOrNull(node, "content_language", "contentLanguage", "language", "langue"),
            textListOrEmpty(node, "risk_labels", "riskLabels", "risks"),
            textListOrEmpty(node, "visited_urls", "visitedUrls", "sources"),
            integerOrNull(node, "credibility_score", "score_credibilite", "score_crédibilité", "score"),
            textOrNull(node, "verdict", "verdict_label", "verdict_final", "conclusion"),
            textOrNull(node, "reasoning", "raisonnement", "explication", "analyse"),
            textListOrEmpty(node, "limitations", "limits", "caveats"),
            textListOrEmpty(node, "recommended_checks", "recommendedChecks", "next_checks")
        );
    }

    private FollowUpAnswer toFollowUp(JsonNode node) {
        return new FollowUpAnswer(
            textOrNull(node, "answer", "response"),
            textListOrEmpty(node, "source_urls", "sourceUrls", "urls"),
            textListOrEmpty(node, "suggested_checks", "suggestedChecks", "recommended_checks")
        );
    }

    private java.util.stream.Stream<JsonNode> stream(JsonNode arrayNode) {
        return java.util.stream.StreamSupport.stream(arrayNode.spliterator(), false);
    }

    private String textOrNull(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText(null);
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private Integer integerOrNull(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isInt() || value.isLong()) {
                return value.asInt();
            }
            if (value.isTextual()) {
                try {
                    return Integer.parseInt(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    // try next alias
                }
            }
        }
        return null;
    }

    private List<String> textListOrEmpty(JsonNode node, String... fieldNames) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isArray()) {
                stream(value)
                    .map(item -> item.isTextual() ? item.asText() : item.toString())
                    .map(text -> text == null ? "" : text.trim())
                    .filter(text -> !text.isBlank())
                    .forEach(values::add);
            } else if (value.isTextual()) {
                String text = value.asText("").trim();
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
        }
        return List.copyOf(values);
    }

    private String extractContent(JsonNode contentNode) {
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    builder.append(textNode.asText());
                } else if (item.isTextual()) {
                    builder.append(item.asText());
                }
            }
            return builder.toString();
        }
        return contentNode.toString();
    }

    private String normalizeJsonPayload(String content) throws JsonProcessingException {
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
            trimmed = trimmed.trim();
        }

        if (looksLikeJsonObject(trimmed)) {
            return trimmed;
        }

        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            String candidate = trimmed.substring(objectStart, objectEnd + 1).trim();
            if (looksLikeJsonObject(candidate)) {
                return candidate;
            }
        }

        JsonNode parsedNode = objectMapper.readTree(trimmed);
        if (parsedNode.isTextual()) {
            String nested = parsedNode.asText().trim();
            if (looksLikeJsonObject(nested)) {
                return nested;
            }
        }
        return trimmed;
    }

    private String repairLikelyTruncatedJson(String value) {
        if (value == null || value.isBlank() || value.charAt(0) != '{') {
            return value;
        }

        StringBuilder builder = new StringBuilder(value);
        boolean inString = false;
        boolean escaping = false;
        int openBraces = 0;
        int openBrackets = 0;

        for (int index = 0; index < builder.length(); index++) {
            char current = builder.charAt(index);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (current == '\\') {
                escaping = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                openBraces++;
            } else if (current == '}') {
                openBraces = Math.max(0, openBraces - 1);
            } else if (current == '[') {
                openBrackets++;
            } else if (current == ']') {
                openBrackets = Math.max(0, openBrackets - 1);
            }
        }

        if (inString) {
            builder.append('"');
        }
        while (openBrackets-- > 0) {
            builder.append(']');
        }
        while (openBraces-- > 0) {
            builder.append('}');
        }
        return builder.toString();
    }

    private boolean looksLikeJsonObject(String value) {
        return value.startsWith("{") && value.endsWith("}");
    }

    private PromptFactory.PromptBundle appendRetryInstruction(PromptFactory.PromptBundle prompt) {
        List<PromptFactory.Message> retriedMessages = new ArrayList<>(prompt.messages());
        retriedMessages.add(new PromptFactory.Message(
            "user",
            "Your previous answer was invalid or truncated. Return the full JSON object only. Keep each field concise."
        ));
        return new PromptFactory.PromptBundle(List.copyOf(retriedMessages));
    }

    private String preview(String value) {
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 240 ? compact : compact.substring(0, 240) + "...";
    }

    private Map<String, Object> signalExtractionSchema() {
        return Map.ofEntries(
            Map.entry("type", "object"),
            Map.entry("additionalProperties", false),
            Map.entry("required", List.of("summary", "claims", "entities", "content_language", "risk_labels")),
            Map.entry("properties", Map.ofEntries(
                Map.entry("summary", Map.of(
                    "type", "string",
                    "description", "High-level summary of the submission"
                )),
                Map.entry("claims", Map.ofEntries(
                    Map.entry("type", "array"),
                    Map.entry("items", Map.ofEntries(
                        Map.entry("type", "object"),
                        Map.entry("additionalProperties", false),
                        Map.entry("required", List.of("statement", "why_it_matters")),
                        Map.entry("properties", Map.of(
                            "statement", Map.of("type", "string"),
                            "why_it_matters", Map.of("type", "string")
                        ))
                    ))
                )),
                Map.entry("entities", entityArraySchema()),
                Map.entry("content_language", Map.of(
                    "type", "string",
                    "description", "Best-effort language label for the content"
                )),
                Map.entry("risk_labels", stringArraySchema("Short labels that describe the investigation risk"))
            ))
        );
    }

    private Map<String, Object> investigationSchema() {
        return Map.ofEntries(
            Map.entry("type", "object"),
            Map.entry("additionalProperties", false),
            Map.entry("required", List.of(
                "summary",
                "claims",
                "entities",
                "content_language",
                "risk_labels",
                "visited_urls",
                "credibility_score",
                "verdict",
                "reasoning",
                "limitations",
                "recommended_checks"
            )),
            Map.entry("properties", Map.ofEntries(
                Map.entry("summary", Map.of(
                    "type", "string",
                    "description", "Short journalistic summary of the content"
                )),
                Map.entry("claims", Map.ofEntries(
                    Map.entry("type", "array"),
                    Map.entry("items", Map.ofEntries(
                        Map.entry("type", "object"),
                        Map.entry("additionalProperties", false),
                        Map.entry("required", List.of(
                            "statement",
                            "suspicion",
                            "evidence_hint",
                            "assessment",
                            "confidence",
                            "evidence_points",
                            "source_urls",
                            "next_step"
                        )),
                        Map.entry("properties", Map.ofEntries(
                            Map.entry("statement", Map.of("type", "string")),
                            Map.entry("suspicion", Map.of("type", "string")),
                            Map.entry("evidence_hint", Map.of("type", "string")),
                            Map.entry("assessment", Map.of("type", "string", "enum", CLAIM_ASSESSMENTS)),
                            Map.entry("confidence", Map.of("type", "integer", "minimum", 0, "maximum", 100)),
                            Map.entry("evidence_points", stringArraySchema("Concise supporting or contradictory evidence points")),
                            Map.entry("source_urls", stringArraySchema("Best source URLs tied to this claim")),
                            Map.entry("next_step", Map.of("type", "string"))
                        ))
                    ))
                )),
                Map.entry("entities", entityArraySchema()),
                Map.entry("content_language", Map.of("type", "string")),
                Map.entry("risk_labels", stringArraySchema("Short labels that describe the investigation risk")),
                Map.entry("visited_urls", stringArraySchema("All source URLs that materially informed the investigation")),
                Map.entry("credibility_score", Map.of(
                    "type", "integer",
                    "minimum", 0,
                    "maximum", 100
                )),
                Map.entry("verdict", Map.of(
                    "type", "string",
                    "enum", List.of("Likely Fake", "Questionable", "Likely True", "Verified")
                )),
                Map.entry("reasoning", Map.of(
                    "type", "string",
                    "description", "Main explanation with red flags and supporting signals"
                )),
                Map.entry("limitations", stringArraySchema("Important caveats that prevent overclaiming certainty")),
                Map.entry("recommended_checks", stringArraySchema("Concrete next verification steps"))
            ))
        );
    }

    private Map<String, Object> followUpSchema() {
        return Map.ofEntries(
            Map.entry("type", "object"),
            Map.entry("additionalProperties", false),
            Map.entry("required", List.of("answer", "source_urls", "suggested_checks")),
            Map.entry("properties", Map.ofEntries(
                Map.entry("answer", Map.of(
                    "type", "string",
                    "description", "Grounded answer to the user's follow-up question"
                )),
                Map.entry("source_urls", stringArraySchema("Best URLs from the report that support the answer")),
                Map.entry("suggested_checks", stringArraySchema("Concrete next checks the user should perform"))
            ))
        );
    }

    private Map<String, Object> entityArraySchema() {
        return Map.ofEntries(
            Map.entry("type", "array"),
            Map.entry("items", Map.ofEntries(
                Map.entry("type", "object"),
                Map.entry("additionalProperties", false),
                Map.entry("required", List.of("name", "type", "context")),
                Map.entry("properties", Map.of(
                    "name", Map.of("type", "string"),
                    "type", Map.of("type", "string"),
                    "context", Map.of("type", "string")
                ))
            ))
        );
    }

    private Map<String, Object> stringArraySchema(String description) {
        return Map.of(
            "type", "array",
            "description", description,
            "items", Map.of("type", "string")
        );
    }

    public record AiAnalysis(AsiInvestigationResult result, String modelUsed) {
    }

    public record FollowUpAnswer(String answer, List<String> sourceUrls, List<String> suggestedChecks) {
    }

    private record StructuredJsonResponse(JsonNode payload, String modelUsed) {
    }

    private static final class InvalidStructuredOutputException extends RuntimeException {
        private InvalidStructuredOutputException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
