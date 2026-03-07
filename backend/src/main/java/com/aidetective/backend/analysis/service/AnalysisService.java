package com.aidetective.backend.analysis.service;

import com.aidetective.backend.analysis.dto.AnalysisHistoryItemResponse;
import com.aidetective.backend.analysis.dto.AnalysisResponse;
import com.aidetective.backend.analysis.dto.AnalyzeRequest;
import com.aidetective.backend.analysis.dto.AsiInvestigationResult;
import com.aidetective.backend.analysis.dto.AsiSignalExtractionResult;
import com.aidetective.backend.analysis.dto.FollowUpQuestionRequest;
import com.aidetective.backend.analysis.dto.FollowUpResponse;
import com.aidetective.backend.analysis.dto.ClaimResponse;
import com.aidetective.backend.analysis.dto.EntityResponse;
import com.aidetective.backend.analysis.exception.AnalysisNotFoundException;
import com.aidetective.backend.analysis.exception.BadRequestException;
import com.aidetective.backend.analysis.integration.AsiOneClient;
import com.aidetective.backend.analysis.model.AnalysisRecord;
import com.aidetective.backend.analysis.model.AnalysisStatus;
import com.aidetective.backend.analysis.model.ClaimInsight;
import com.aidetective.backend.analysis.model.EntityInsight;
import com.aidetective.backend.analysis.model.InputType;
import com.aidetective.backend.analysis.model.Verdict;
import com.aidetective.backend.analysis.repository.AnalysisRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnalysisService {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s)\\]}>\"']+", Pattern.CASE_INSENSITIVE);

    private final AnalysisRepository analysisRepository;
    private final ArticleExtractionService articleExtractionService;
    private final AsiOneClient asiOneClient;
    private final DocumentExtractionService documentExtractionService;

    public AnalysisService(
        AnalysisRepository analysisRepository,
        ArticleExtractionService articleExtractionService,
        AsiOneClient asiOneClient,
        DocumentExtractionService documentExtractionService
    ) {
        this.analysisRepository = analysisRepository;
        this.articleExtractionService = articleExtractionService;
        this.asiOneClient = asiOneClient;
        this.documentExtractionService = documentExtractionService;
    }

    public AnalysisResponse analyze(AnalyzeRequest request) {
        boolean hasText = hasValue(request.text());
        boolean hasUrl = hasValue(request.url());
        boolean hasImage = hasValue(request.imageBase64());
        boolean hasVideo = hasValue(request.videoUrl());

        int providedInputs = (hasText ? 1 : 0) + (hasUrl ? 1 : 0) + (hasImage ? 1 : 0) + (hasVideo ? 1 : 0);
        if (providedInputs != 1) {
            throw new BadRequestException("Provide exactly one input: text, URL, image, or video URL.");
        }

        InputType inputType;
        String sourceUrl = null;
        String sourceTitle = null;
        String sourceLabel;
        String normalizedText = null;
        String originalInput;
        String imageMimeType = null;
        List<String> explicitVisitedUrls = new ArrayList<>();
        AsiSignalExtractionResult extraction;
        AsiOneClient.AiAnalysis aiAnalysis;

        if (hasUrl) {
            inputType = InputType.URL;
            var extractedArticle = articleExtractionService.extract(request.url());
            sourceUrl = extractedArticle.url();
            sourceTitle = extractedArticle.title();
            sourceLabel = sourceTitle == null || sourceTitle.isBlank() ? sourceUrl : sourceTitle;
            normalizedText = normalize(request.url(), extractedArticle.text());
            originalInput = sourceUrl;
            explicitVisitedUrls.add(sourceUrl);
            extraction = asiOneClient.extractSignals(normalizedText, inputType, sourceUrl, sourceTitle, sourceLabel);
            aiAnalysis = asiOneClient.analyzeInvestigation(normalizedText, inputType, sourceUrl, sourceTitle, sourceLabel, extraction);
        } else if (hasImage) {
            inputType = InputType.IMAGE;
            imageMimeType = normalizeImageMimeType(request.imageMimeType());
            String imageBase64 = sanitizeBase64(request.imageBase64());
            validateBase64(imageBase64);
            sourceTitle = hasValue(request.imageName()) ? request.imageName().trim() : "Uploaded image";
            sourceLabel = sourceTitle;
            originalInput = sourceTitle;
            extraction = asiOneClient.extractImageSignals(imageBase64, imageMimeType, sourceTitle, sourceLabel);
            aiAnalysis = asiOneClient.analyzeInvestigation(null, inputType, null, sourceTitle, sourceLabel, extraction);
        } else if (hasVideo) {
            inputType = InputType.VIDEO;
            sourceUrl = normalizeVideoUrl(request.videoUrl());
            sourceTitle = videoTitle(sourceUrl);
            sourceLabel = sourceTitle;
            originalInput = sourceUrl;
            normalizedText = """
                Investigate this video source for credibility.
                Video URL: %s
                Analyze the title, source host, likely context, and any suspicious framing based on the URL and surrounding metadata.
                If direct video contents are unavailable, explain the limitations clearly and assess the credibility signals of the source itself.
                """.formatted(sourceUrl);
            explicitVisitedUrls.add(sourceUrl);
            extraction = asiOneClient.extractSignals(normalizedText, inputType, sourceUrl, sourceTitle, sourceLabel);
            aiAnalysis = asiOneClient.analyzeInvestigation(normalizedText, inputType, sourceUrl, sourceTitle, sourceLabel, extraction);
        } else {
            inputType = InputType.TEXT;
            sourceLabel = "Manual text submission";
            normalizedText = normalize("text", request.text());
            originalInput = request.text().trim();
            extraction = asiOneClient.extractSignals(normalizedText, inputType, null, null, sourceLabel);
            aiAnalysis = asiOneClient.analyzeInvestigation(normalizedText, inputType, null, null, sourceLabel, extraction);
        }

        var record = toRecord(
            aiAnalysis,
            extraction,
            inputType,
            originalInput,
            sourceUrl,
            sourceTitle,
            sourceLabel,
            normalizedText,
            imageMimeType,
            explicitVisitedUrls
        );
        var saved = analysisRepository.save(record);
        return toResponse(saved);
    }

    public AnalysisResponse analyzeUpload(MultipartFile file, boolean ocrEnabled) {
        var extractedUpload = documentExtractionService.extract(file, ocrEnabled);
        InputType inputType = extractedUpload.inputType();
        String sourceTitle = extractedUpload.sourceTitle();
        String sourceLabel = extractedUpload.sourceLabel();
        String mimeType = extractedUpload.mimeType();
        String normalizedText = normalize(inputType.name().toLowerCase(), extractedUpload.normalizedText());

        AsiSignalExtractionResult extraction = asiOneClient.extractSignals(
            normalizedText,
            inputType,
            null,
            sourceTitle,
            sourceLabel
        );
        AsiOneClient.AiAnalysis aiAnalysis = asiOneClient.analyzeInvestigation(
            normalizedText,
            inputType,
            null,
            sourceTitle,
            sourceLabel,
            extraction
        );

        var record = toRecord(
            aiAnalysis,
            extraction,
            inputType,
            sourceTitle,
            null,
            sourceTitle,
            sourceLabel,
            normalizedText,
            inputType == InputType.IMAGE ? mimeType : null,
            List.of()
        );
        var saved = analysisRepository.save(record);
        return toResponse(saved);
    }

    public List<AnalysisHistoryItemResponse> getHistory() {
        return analysisRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50)).stream()
            .map(this::toHistoryItem)
            .toList();
    }

    public AnalysisResponse getById(String id) {
        return analysisRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new AnalysisNotFoundException(id));
    }

    public FollowUpResponse followUp(String id, FollowUpQuestionRequest request) {
        AnalysisRecord record = analysisRepository.findById(id)
            .orElseThrow(() -> new AnalysisNotFoundException(id));

        String question = normalize("follow-up question", request.question());
        var followUp = asiOneClient.followUp(record, question);

        return new FollowUpResponse(
            defaultText(followUp.answer(), "No follow-up answer returned by the AI model."),
            cleanList(followUp.sourceUrls()),
            selectList(
                cleanList(followUp.suggestedChecks()),
                List.of("Open the strongest cited source and compare it against the claim wording directly.")
            )
        );
    }

    private AnalysisRecord toRecord(
        AsiOneClient.AiAnalysis aiAnalysis,
        AsiSignalExtractionResult extraction,
        InputType inputType,
        String originalInput,
        String sourceUrl,
        String sourceTitle,
        String sourceLabel,
        String normalizedText,
        String imageMimeType,
        List<String> explicitVisitedUrls
    ) {
        AsiInvestigationResult result = aiAnalysis.result();
        AnalysisResolution resolution = resolveAnalysisResolution(inputType, result);

        var record = new AnalysisRecord();
        record.setInputType(inputType);
        record.setOriginalInput(originalInput);
        record.setSourceUrl(sourceUrl);
        record.setSourceTitle(sourceTitle);
        record.setSourceLabel(sourceLabel);
        record.setImageMimeType(imageMimeType);
        record.setModelUsed(defaultText(aiAnalysis.modelUsed(), "asi1"));
        record.setAnalysisStatus(resolution.status());
        record.setStatusReason(resolution.reason());
        record.setContentLanguage(defaultText(result.content_language(), defaultText(extraction.content_language(), "Unknown")));
        record.setRiskLabels(selectList(result.risk_labels(), extraction.risk_labels()));
        record.setVisitedUrls(extractVisitedUrls(explicitVisitedUrls, originalInput, normalizedText, result));
        record.setNormalizedText(normalizedText);
        record.setSummary(defaultText(result.summary(), defaultText(extraction.summary(), "No summary returned by the AI model.")));
        record.setClaims(mapClaims(result.claims(), extraction.claims()));
        record.setEntities(mapEntities(result.entities(), extraction.entities()));
        record.setCredibilityScore(resolution.score());
        record.setVerdict(resolution.verdict());
        record.setReasoning(defaultText(result.reasoning(), "No reasoning returned by the AI model."));
        record.setLimitations(selectList(result.limitations(), defaultLimitations(inputType, resolution)));
        record.setRecommendedChecks(selectList(result.recommended_checks(), defaultRecommendedChecks(inputType, resolution)));
        record.setCreatedAt(Instant.now());
        return record;
    }

    private List<String> extractVisitedUrls(
        List<String> explicitVisitedUrls,
        String originalInput,
        String normalizedText,
        AsiInvestigationResult result
    ) {
        LinkedHashSet<String> collected = new LinkedHashSet<>();
        addUrls(collected, explicitVisitedUrls == null ? List.of() : explicitVisitedUrls);
        addUrls(collected, result.visited_urls());
        addUrls(collected, originalInput);
        addUrls(collected, normalizedText);
        addUrls(collected, result.summary());
        addUrls(collected, result.reasoning());
        addUrls(collected, result.limitations());
        addUrls(collected, result.recommended_checks());

        if (result.claims() != null) {
            for (AsiInvestigationResult.AsiClaim claim : result.claims()) {
                addUrls(collected, claim.source_urls());
                addUrls(collected, claim.statement());
                addUrls(collected, claim.suspicion());
                addUrls(collected, claim.evidence_hint());
                addUrls(collected, claim.evidence_points());
                addUrls(collected, claim.next_step());
            }
        }

        return List.copyOf(collected);
    }

    private void addUrls(LinkedHashSet<String> target, List<String> urls) {
        if (urls == null) {
            return;
        }
        for (String url : urls) {
            addUrls(target, url);
        }
    }

    private void addUrls(LinkedHashSet<String> target, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            target.add(stripTrailingPunctuation(matcher.group()));
        }
    }

    private String stripTrailingPunctuation(String url) {
        return url.replaceAll("[.,;:!?]+$", "");
    }

    private List<ClaimInsight> mapClaims(
        List<AsiInvestigationResult.AsiClaim> claims,
        List<AsiSignalExtractionResult.AsiClaimCandidate> fallbackClaims
    ) {
        List<AsiInvestigationResult.AsiClaim> safeClaims = claims == null ? List.of() : claims;
        if (safeClaims.isEmpty() && (fallbackClaims == null || fallbackClaims.isEmpty())) {
            return Collections.emptyList();
        }

        List<ClaimInsight> mapped = new ArrayList<>();
        int count = Math.max(safeClaims.size(), fallbackClaims == null ? 0 : fallbackClaims.size());
        for (int index = 0; index < count; index++) {
            AsiInvestigationResult.AsiClaim claim = index < safeClaims.size() ? safeClaims.get(index) : null;
            AsiSignalExtractionResult.AsiClaimCandidate fallback = fallbackClaims != null && index < fallbackClaims.size()
                ? fallbackClaims.get(index)
                : null;

            mapped.add(new ClaimInsight(
                defaultText(claim == null ? null : claim.statement(), defaultText(fallback == null ? null : fallback.statement(), "Unspecified claim")),
                defaultText(claim == null ? null : claim.suspicion(), "No suspicion detail provided"),
                defaultText(claim == null ? null : claim.evidence_hint(), defaultText(fallback == null ? null : fallback.why_it_matters(), "No evidence hint provided")),
                defaultText(claim == null ? null : claim.assessment(), "Questionable"),
                clampScore(claim == null ? null : claim.confidence()),
                cleanList(claim == null ? null : claim.evidence_points()),
                cleanList(claim == null ? null : claim.source_urls()),
                defaultText(claim == null ? null : claim.next_step(), "Cross-check this claim with an official or primary source.")
            ));
        }
        return mapped;
    }

    private List<EntityInsight> mapEntities(
        List<AsiInvestigationResult.AsiEntity> entities,
        List<AsiSignalExtractionResult.AsiEntity> fallbackEntities
    ) {
        List<AsiInvestigationResult.AsiEntity> safeEntities = entities == null ? List.of() : entities;
        if (safeEntities.isEmpty() && (fallbackEntities == null || fallbackEntities.isEmpty())) {
            return Collections.emptyList();
        }

        List<EntityInsight> mapped = new ArrayList<>();
        for (AsiInvestigationResult.AsiEntity entity : safeEntities) {
            mapped.add(new EntityInsight(
                defaultText(entity.name(), "Unknown"),
                defaultText(entity.type(), "Unclassified"),
                defaultText(entity.context(), "No context provided")
            ));
        }

        if (!mapped.isEmpty()) {
            return mapped;
        }

        return fallbackEntities.stream()
            .map(entity -> new EntityInsight(
                defaultText(entity.name(), "Unknown"),
                defaultText(entity.type(), "Unclassified"),
                defaultText(entity.context(), "No context provided")
            ))
            .toList();
    }

    private AnalysisResponse toResponse(AnalysisRecord record) {
        AnalysisResolution resolution = resolveStoredRecordResolution(record);
        return new AnalysisResponse(
            record.getId(),
            record.getInputType(),
            record.getSourceUrl(),
            record.getSourceTitle(),
            record.getSourceLabel(),
            record.getImageMimeType(),
            record.getModelUsed(),
            resolution.status(),
            resolution.reason(),
            record.getContentLanguage(),
            record.getRiskLabels(),
            record.getVisitedUrls(),
            record.getSummary(),
            record.getClaims().stream().map(claim -> new ClaimResponse(
                claim.getStatement(),
                claim.getSuspicion(),
                claim.getEvidenceHint(),
                claim.getAssessment(),
                claim.getConfidence(),
                claim.getEvidencePoints(),
                claim.getSourceUrls(),
                claim.getNextStep()
            )).toList(),
            record.getEntities().stream().map(entity -> new EntityResponse(
                entity.getName(),
                entity.getType(),
                entity.getContext()
            )).toList(),
            resolution.score(),
            resolution.verdict() == null ? null : resolution.verdict().getLabel(),
            record.getReasoning(),
            record.getLimitations(),
            record.getRecommendedChecks(),
            record.getCreatedAt()
        );
    }

    private AnalysisHistoryItemResponse toHistoryItem(AnalysisRecord record) {
        AnalysisResolution resolution = resolveStoredRecordResolution(record);
        return new AnalysisHistoryItemResponse(
            record.getId(),
            record.getInputType(),
            record.getSourceUrl(),
            record.getSourceTitle(),
            record.getSourceLabel(),
            record.getModelUsed(),
            resolution.status(),
            resolution.reason(),
            record.getContentLanguage(),
            record.getRiskLabels(),
            record.getSummary(),
            resolution.score(),
            resolution.verdict() == null ? null : resolution.verdict().getLabel(),
            record.getCreatedAt()
        );
    }

    private AnalysisResolution resolveStoredRecordResolution(AnalysisRecord record) {
        AnalysisResolution derived = resolveAnalysisResolution(
            record.getInputType(),
            new AsiInvestigationResult(
                record.getSummary(),
                List.of(),
                List.of(),
                record.getContentLanguage(),
                record.getRiskLabels(),
                record.getVisitedUrls(),
                record.getCredibilityScore(),
                record.getVerdict() == null ? null : record.getVerdict().getLabel(),
                record.getReasoning(),
                record.getLimitations(),
                record.getRecommendedChecks()
            )
        );

        AnalysisStatus status = record.getAnalysisStatus();
        String reason = record.getStatusReason();
        Integer score = clampScore(record.getCredibilityScore());
        Verdict verdict = record.getVerdict();

        boolean shouldDeriveStatus = status == null
            || (record.getInputType() == InputType.VIDEO && derived.status() != AnalysisStatus.COMPLETED)
            || (status == AnalysisStatus.LIMITED && (reason == null || reason.isBlank()));

        if (shouldDeriveStatus) {
            status = derived.status();
            reason = derived.reason();
            if (status == AnalysisStatus.CANNOT_ANALYZE) {
                score = null;
                verdict = null;
            } else {
                score = score == null ? derived.score() : score;
                verdict = verdict == null ? derived.verdict() : verdict;
            }
        }

        if (status == null) {
            status = AnalysisStatus.COMPLETED;
        }
        if (status == AnalysisStatus.CANNOT_ANALYZE) {
            score = null;
            verdict = null;
            reason = defaultText(reason, derived.reason());
        }
        if (status == AnalysisStatus.LIMITED && (reason == null || reason.isBlank())) {
            reason = derived.reason();
        }

        return new AnalysisResolution(status, reason, score, verdict);
    }

    private Integer clampScore(Integer score) {
        if (score == null) {
            return null;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String normalize(String label, String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            throw new BadRequestException("The submitted %s is empty.".formatted(label));
        }
        return normalized;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private List<String> selectList(List<String> primary, List<String> fallback) {
        List<String> cleanedPrimary = cleanList(primary);
        return cleanedPrimary.isEmpty() ? cleanList(fallback) : cleanedPrimary;
    }

    private List<String> cleanList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null) {
                String normalized = value.trim();
                if (!normalized.isBlank()) {
                    cleaned.add(normalized);
                }
            }
        }
        return List.copyOf(cleaned);
    }

    private List<String> defaultLimitations(InputType inputType, AnalysisResolution resolution) {
        List<String> limitations = new ArrayList<>();
        if (resolution.status() == AnalysisStatus.CANNOT_ANALYZE) {
            limitations.add(defaultText(resolution.reason(), "The submission could not be analyzed reliably."));
            limitations.add("No credibility score or verdict should be treated as final for this submission.");
            return limitations;
        }

        limitations.add("The credibility score is a decision-support signal, not a substitute for primary-source verification.");
        if (inputType == InputType.IMAGE) {
            limitations.add("Visual analysis may miss manipulation that requires forensic inspection or original file metadata.");
        }
        if (inputType == InputType.PDF || inputType == InputType.VIDEO) {
            limitations.add("The investigation may rely on extracted text or file metadata when the original media cannot be fully inspected.");
        }
        if (inputType == InputType.URL) {
            limitations.add("A fetched article can change after publication, so archived or primary copies may still be needed.");
        }
        return limitations;
    }

    private List<String> defaultRecommendedChecks(InputType inputType, AnalysisResolution resolution) {
        List<String> checks = new ArrayList<>();
        if (resolution.status() == AnalysisStatus.CANNOT_ANALYZE && inputType == InputType.VIDEO) {
            checks.add("Confirm that the video URL is public, not deleted, and accessible without platform restrictions.");
            checks.add("Provide a working link, transcript, or uploaded clip so the system can inspect actual content.");
            checks.add("Cross-check the claimed video title or uploader identity against the original platform page.");
            return checks;
        }

        checks.add("Compare the strongest claim against an official, primary, or institution-backed source.");
        checks.add("Open at least one cited URL and confirm the evidence matches the wording in the claim.");
        if (inputType == InputType.IMAGE) {
            checks.add("Run a reverse image search or inspect the original upload metadata if authenticity matters.");
        }
        if (inputType == InputType.PDF) {
            checks.add("Verify the document origin, letterhead, signatures, and whether the PDF was OCR-only.");
        }
        if (inputType == InputType.VIDEO) {
            checks.add("Confirm the uploader, upload date, and whether the clip is cropped or taken out of context.");
        }
        return checks;
    }

    private AnalysisResolution resolveAnalysisResolution(InputType inputType, AsiInvestigationResult result) {
        Integer score = clampScore(result.credibility_score());
        Verdict verdict = null;
        if (score != null) {
            verdict = Verdict.fromLabel(result.verdict());
            if (verdict == null) {
                verdict = Verdict.fromScore(score);
            }
        }

        if (inputType != InputType.VIDEO) {
            return new AnalysisResolution(AnalysisStatus.COMPLETED, null, score == null ? 50 : score, verdict == null ? Verdict.QUESTIONABLE : verdict);
        }

        String combined = ((result.summary() == null ? "" : result.summary()) + " " + (result.reasoning() == null ? "" : result.reasoning())).toLowerCase();

        if (containsAny(combined,
            "404",
            "broken link",
            "video is inaccessible",
            "video inaccessible",
            "returns a 404",
            "cannot be fact-checked",
            "no video content",
            "no metadata",
            "could not be retrieved",
            "unavailable"
        )) {
            return new AnalysisResolution(
                AnalysisStatus.CANNOT_ANALYZE,
                "The video URL is inaccessible or does not expose enough retrievable metadata to analyze the actual content.",
                null,
                null
            );
        }

        if (containsAny(combined,
            "frames or transcript are unavailable",
            "metadata only",
            "actual video frames",
            "transcript unavailable",
            "source itself"
        )) {
            return new AnalysisResolution(
                AnalysisStatus.LIMITED,
                "Only limited video metadata was available, so this result should be treated as provisional.",
                score == null ? 50 : score,
                verdict == null ? Verdict.QUESTIONABLE : verdict
            );
        }

        return new AnalysisResolution(
            AnalysisStatus.COMPLETED,
            null,
            score == null ? 50 : score,
            verdict == null ? Verdict.QUESTIONABLE : verdict
        );
    }

    private boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private String sanitizeBase64(String imageBase64) {
        return imageBase64 == null ? "" : imageBase64.trim();
    }

    private void validateBase64(String imageBase64) {
        try {
            Base64.getDecoder().decode(imageBase64);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("The uploaded image is not valid base64 content.");
        }
    }

    private String normalizeImageMimeType(String imageMimeType) {
        String normalized = imageMimeType == null ? "" : imageMimeType.trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new BadRequestException("Please provide an image MIME type.");
        }
        return normalized.equals("image/jpg") ? "image/jpeg" : normalized;
    }

    private String normalizeVideoUrl(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new BadRequestException("Only HTTP and HTTPS video URLs are supported.");
            }
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Please provide a valid video URL.");
        }
    }

    private String videoTitle(String videoUrl) {
        try {
            URI uri = URI.create(videoUrl);
            String host = uri.getHost() == null ? "Video source" : uri.getHost();
            return "Video analysis from %s".formatted(host);
        } catch (IllegalArgumentException exception) {
            return "Video source";
        }
    }

    private record AnalysisResolution(AnalysisStatus status, String reason, Integer score, Verdict verdict) {
    }
}
