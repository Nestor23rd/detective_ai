package com.aidetective.backend.analysis.service;

import com.aidetective.backend.analysis.dto.AsiSignalExtractionResult;
import com.aidetective.backend.analysis.model.AnalysisRecord;
import com.aidetective.backend.analysis.model.InputType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PromptFactory {

    public PromptBundle createSignalExtractionText(
        String normalizedText,
        InputType inputType,
        String sourceUrl,
        String sourceTitle,
        String sourceLabel
    ) {
        return new PromptBundle(
            List.of(
                new Message("system", systemPrompt()),
                new Message("user", extractionTextPrompt(normalizedText, inputType, sourceUrl, sourceTitle, sourceLabel))
            )
        );
    }

    public PromptBundle createSignalExtractionImage(String imageMimeType, String imageBase64, String sourceTitle, String sourceLabel) {
        String context = """
            First-pass extraction for an uploaded image.

            Context:
            - input_type: IMAGE
            - source_title: %s
            - source_url: N/A
            - source_label: %s

            Required tasks:
            1. Summarize what the image claims, implies, or visually suggests.
            2. Extract the main factual or visual claims visible in the image.
            3. For each extracted claim, explain briefly why it matters for a second-pass verification.
            4. Extract named entities, organizations, locations, concepts, logos, dates, and statistics when present.
            5. Identify the likely content language. Use labels such as English, Hindi, Hinglish, Bengali, or Mixed.
            6. Add 2-4 short risk labels such as misinformation, financial scam, impersonation, public safety, politics, health, or manipulated media.
            """.formatted(
            sourceTitle == null || sourceTitle.isBlank() ? "Uploaded image" : sourceTitle,
            sourceLabel == null || sourceLabel.isBlank() ? "Image upload" : sourceLabel
        );

        return new PromptBundle(
            List.of(
                new Message("system", systemPrompt()),
                new Message("user", List.of(
                    Map.of("type", "text", "text", context),
                    Map.of("type", "image_url", "image_url", Map.of(
                        "url", "data:%s;base64,%s".formatted(imageMimeType, imageBase64)
                    ))
                ))
            )
        );
    }

    public PromptBundle createVerification(
        String normalizedText,
        InputType inputType,
        String sourceUrl,
        String sourceTitle,
        String sourceLabel,
        AsiSignalExtractionResult extraction
    ) {
        return new PromptBundle(
            List.of(
                new Message("system", systemPrompt()),
                new Message("user", verificationPrompt(normalizedText, inputType, sourceUrl, sourceTitle, sourceLabel, extraction))
            )
        );
    }

    public PromptBundle createFollowUp(AnalysisRecord record, String question) {
        return new PromptBundle(
            List.of(
                new Message("system", systemPrompt()),
                new Message("user", followUpPrompt(record, question))
            )
        );
    }

    private String systemPrompt() {
        return """
            You are AI Internet Detective, an investigative journalist and senior fact-checker.
            Examine the submission critically.
            Focus on verifiable claims, misleading framing, sensational language, missing evidence, conspiracy patterns,
            unrealistic science, manipulation tactics, and credibility signals.
            Handle multilingual internet content, including English, Hindi, Hinglish, and code-mixed posts.
            Treat WhatsApp forwards, screenshots, short videos, flyers, and low-context PDFs as high-risk formats that need careful caveats.
            If the submission is an image, inspect visible text, scene details, logos, captions, formatting, and signs of manipulation.
            Respond only with valid JSON that matches the required schema.
            Use concise but concrete language.
            """;
    }

    private String extractionTextPrompt(
        String normalizedText,
        InputType inputType,
        String sourceUrl,
        String sourceTitle,
        String sourceLabel
    ) {
        if (inputType == InputType.VIDEO) {
            return videoExtractionPrompt(normalizedText, sourceUrl, sourceTitle, sourceLabel);
        }

        return """
            First-pass extraction for the following submission.

            Context:
            - input_type: %s
            - source_title: %s
            - source_url: %s
            - source_label: %s

            Required tasks:
            1. Summarize the content in 2-3 sentences.
            2. Extract the most important factual claims.
            3. For each extracted claim, explain briefly why it matters for a second-pass verification.
            4. Extract named entities, organizations, locations, concepts, and statistics when present.
            5. Identify the likely content language. Use labels such as English, Hindi, Hinglish, Bengali, or Mixed.
            6. Add 2-4 short risk labels such as misinformation, financial scam, impersonation, public safety, politics, health, or manipulated media.

            Submission:
            %s
            """.formatted(
            inputType.name(),
            sourceTitle == null || sourceTitle.isBlank() ? "Manual submission" : sourceTitle,
            sourceUrl == null || sourceUrl.isBlank() ? "N/A" : sourceUrl,
            sourceLabel == null || sourceLabel.isBlank() ? "Manual submission" : sourceLabel,
            normalizedText
        );
    }

    private String verificationPrompt(
        String normalizedText,
        InputType inputType,
        String sourceUrl,
        String sourceTitle,
        String sourceLabel,
        AsiSignalExtractionResult extraction
    ) {
        if (inputType == InputType.VIDEO) {
            return videoVerificationPrompt(normalizedText, sourceUrl, sourceTitle, sourceLabel, extraction);
        }

        return """
            Second-pass verification for the following submission.

            Context:
            - input_type: %s
            - source_title: %s
            - source_url: %s
            - source_label: %s
            - extracted_content_language: %s
            - extracted_risk_labels: %s

            First-pass summary:
            %s

            First-pass claims:
            %s

            First-pass entities:
            %s

            Original submission excerpt:
            %s

            Required tasks:
            1. Produce a tighter investigation summary for judges and end users.
            2. Verify each extracted claim with a concrete assessment chosen from: Supported, Questionable, Contradicted, Insufficient Evidence.
            3. Give each claim a confidence score from 0 to 100.
            4. Add 1-3 evidence points per claim. Each point must be short and specific.
            5. Add direct source URLs per claim whenever you can. If no reliable URL is available, return an empty list and say so in the evidence point or next step.
            6. Keep the overall verdict strictly to one of: Likely Fake, Questionable, Likely True, Verified.
            7. Return 2-4 risk labels, the best-effort content language, limitations, and recommended next checks.
            8. If the content cannot be fully validated, make the uncertainty explicit and lower the confidence.
            9. When web search helps, use it and capture the most relevant URLs.
            10. Fill advanced_modules with:
                - virality_level: Faible/Moyen/Élevé
                - virality_reasons and sharing_factors
                - manipulation_signals (clickbait, fear, urgency, conspiracy, anti-institution, emotional manipulation, false authority)
                - misinformation_impact and impact_reason (Faible/Moyen/Élevé)
                - trust_risk_level and trust_conclusion (Faible/Moyen/Élevé)
                - technical_risks (phishing, malware, scam, illegal streaming, copyright abuse when relevant)
                - security_recommendations and human_verification_steps
            """.formatted(
            inputType.name(),
            safeText(sourceTitle, "Manual submission"),
            safeText(sourceUrl, "N/A"),
            safeText(sourceLabel, "Manual submission"),
            safeText(extraction.content_language(), "Unknown"),
            joinList(extraction.risk_labels(), ", "),
            safeText(extraction.summary(), "No extraction summary available."),
            joinClaimCandidates(extraction.claims()),
            joinEntities(extraction.entities()),
            safeText(normalizedText, "Original submission was image-first or unavailable in text form.")
        );
    }

    private String videoExtractionPrompt(
        String normalizedText,
        String sourceUrl,
        String sourceTitle,
        String sourceLabel
    ) {
        return """
            First-pass extraction for a video link or video metadata submission.

            Context:
            - input_type: VIDEO
            - source_title: %s
            - source_url: %s
            - source_label: %s

            Required tasks:
            1. Summarize what can be inferred from the video URL, title, host, and visible metadata only.
            2. Extract at most 2 important claims.
            3. For each claim, explain briefly why it matters for a second-pass verification.
            4. Extract named entities, channels, organizations, locations, concepts, and statistics when present.
            5. Identify the likely content language. Use labels such as English, Hindi, Hinglish, Bengali, or Mixed.
            6. Add 2-4 short risk labels such as misinformation, impersonation, politics, public safety, or manipulated media.
            7. If the actual video frames or transcript are unavailable, do not speculate about unseen content.

            Submission:
            %s
            """.formatted(
            safeText(sourceTitle, "Video source"),
            safeText(sourceUrl, "N/A"),
            safeText(sourceLabel, "Video source"),
            safeText(normalizedText, "No video metadata was provided.")
        );
    }

    private String videoVerificationPrompt(
        String normalizedText,
        String sourceUrl,
        String sourceTitle,
        String sourceLabel,
        AsiSignalExtractionResult extraction
    ) {
        return """
            Second-pass verification for a video link or metadata-based submission.

            Context:
            - input_type: VIDEO
            - source_title: %s
            - source_url: %s
            - source_label: %s
            - extracted_content_language: %s
            - extracted_risk_labels: %s

            First-pass summary:
            %s

            First-pass claims:
            %s

            First-pass entities:
            %s

            Original submission excerpt:
            %s

            Required tasks:
            1. Return a concise investigation summary.
            2. Verify at most 2 claims.
            3. Focus on source provenance, uploader/channel identity, title wording, available metadata, and surrounding context.
            4. If you cannot inspect the actual video frames or transcript, say so clearly and avoid speculation.
            5. For each claim, return short evidence points and short source URLs only when they materially help.
            6. Keep every string concise so the final JSON is compact and complete.
            7. Keep the overall verdict strictly to one of: Likely Fake, Questionable, Likely True, Verified.
            8. Return limitations and recommended next checks.
            9. Fill advanced_modules in the same structure as text investigations. If technical evidence is missing, keep technical_risks as an empty array.
            """.formatted(
            safeText(sourceTitle, "Video source"),
            safeText(sourceUrl, "N/A"),
            safeText(sourceLabel, "Video source"),
            safeText(extraction.content_language(), "Unknown"),
            joinList(extraction.risk_labels(), ", "),
            safeText(extraction.summary(), "No extraction summary available."),
            joinClaimCandidates(extraction.claims()),
            joinEntities(extraction.entities()),
            safeText(normalizedText, "No video metadata was provided.")
        );
    }

    private String joinClaimCandidates(List<AsiSignalExtractionResult.AsiClaimCandidate> claims) {
        if (claims == null || claims.isEmpty()) {
            return "- No explicit claims were extracted in the first pass.";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < claims.size(); index++) {
            var claim = claims.get(index);
            builder.append(index + 1)
                .append(". statement: ")
                .append(safeText(claim.statement(), "Unspecified claim"))
                .append(" | why_it_matters: ")
                .append(safeText(claim.why_it_matters(), "Requires verification."))
                .append('\n');
        }
        return builder.toString().trim();
    }

    private String joinEntities(List<AsiSignalExtractionResult.AsiEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return "- No entities were extracted in the first pass.";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < entities.size(); index++) {
            var entity = entities.get(index);
            builder.append(index + 1)
                .append(". ")
                .append(safeText(entity.name(), "Unknown"))
                .append(" (")
                .append(safeText(entity.type(), "Unclassified"))
                .append("): ")
                .append(safeText(entity.context(), "No context"))
                .append('\n');
        }
        return builder.toString().trim();
    }

    private String joinList(List<String> values, String separator) {
        if (values == null || values.isEmpty()) {
            return "none";
        }
        return values.stream().map(value -> safeText(value, "")).filter(value -> !value.isBlank()).reduce((left, right) -> left + separator + right).orElse("none");
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String followUpPrompt(AnalysisRecord record, String question) {
        return """
            You are answering a follow-up question about an existing investigation report.
            Stay grounded in the report context below.
            If the question asks for stronger verification, recommend concrete next checks and mention the best URLs already present in the report.
            If the report is uncertain, say so plainly instead of overclaiming.

            Report context:
            - source_title: %s
            - input_type: %s
            - content_language: %s
            - verdict: %s
            - credibility_score: %d
            - risk_labels: %s
            - summary: %s
            - reasoning: %s
            - visited_urls: %s

            Claims:
            %s

            Follow-up question:
            %s
            """.formatted(
            safeText(record.getSourceTitle(), safeText(record.getSourceLabel(), "Manual submission")),
            record.getInputType().name(),
            safeText(record.getContentLanguage(), "Unknown"),
            record.getVerdict().getLabel(),
            record.getCredibilityScore(),
            joinList(record.getRiskLabels(), ", "),
            safeText(record.getSummary(), "No summary available."),
            safeText(record.getReasoning(), "No reasoning available."),
            joinList(record.getVisitedUrls(), ", "),
            joinStoredClaims(record),
            safeText(question, "No question provided.")
        );
    }

    private String joinStoredClaims(AnalysisRecord record) {
        if (record.getClaims() == null || record.getClaims().isEmpty()) {
            return "- No claims were stored for this report.";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < record.getClaims().size(); index++) {
            var claim = record.getClaims().get(index);
            builder.append(index + 1)
                .append(". statement: ")
                .append(safeText(claim.getStatement(), "Unspecified claim"))
                .append(" | assessment: ")
                .append(safeText(claim.getAssessment(), "Questionable"))
                .append(" | confidence: ")
                .append(claim.getConfidence())
                .append(" | evidence_hint: ")
                .append(safeText(claim.getEvidenceHint(), "No evidence hint"))
                .append(" | source_urls: ")
                .append(joinList(claim.getSourceUrls(), ", "))
                .append('\n');
        }
        return builder.toString().trim();
    }

    public record PromptBundle(List<Message> messages) {
    }

    public record Message(String role, Object content) {
    }
}
