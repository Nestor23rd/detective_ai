package com.aidetective.backend.analysis.service;

import com.aidetective.backend.analysis.dto.AnalysisHistoryItemResponse;
import com.aidetective.backend.analysis.dto.AnalysisResponse;
import com.aidetective.backend.analysis.dto.AnalyzeRequest;
import com.aidetective.backend.analysis.dto.AdvancedAnalysisModules;
import com.aidetective.backend.analysis.dto.AsiInvestigationResult;
import com.aidetective.backend.analysis.dto.AsiSignalExtractionResult;
import com.aidetective.backend.analysis.dto.FollowUpQuestionRequest;
import com.aidetective.backend.analysis.dto.FollowUpResponse;
import com.aidetective.backend.analysis.dto.ClaimResponse;
import com.aidetective.backend.analysis.dto.EntityResponse;
import com.aidetective.backend.analysis.exception.AnalysisNotFoundException;
import com.aidetective.backend.analysis.exception.BadRequestException;
import com.aidetective.backend.analysis.integration.GradientAiClient;
import com.aidetective.backend.analysis.model.AnalysisRecord;
import com.aidetective.backend.analysis.model.AnalysisStatus;
import com.aidetective.backend.analysis.model.AdvancedModulesInsight;
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
    private final GradientAiClient gradientAiClient;
    private final DocumentExtractionService documentExtractionService;

    public AnalysisService(
        AnalysisRepository analysisRepository,
        ArticleExtractionService articleExtractionService,
        GradientAiClient gradientAiClient,
        DocumentExtractionService documentExtractionService
    ) {
        this.analysisRepository = analysisRepository;
        this.articleExtractionService = articleExtractionService;
        this.gradientAiClient = gradientAiClient;
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
        GradientAiClient.AiAnalysis aiAnalysis;

        if (hasUrl) {
            inputType = InputType.URL;
            var extractedArticle = articleExtractionService.extract(request.url());
            sourceUrl = extractedArticle.url();
            sourceTitle = extractedArticle.title();
            sourceLabel = sourceTitle == null || sourceTitle.isBlank() ? sourceUrl : sourceTitle;
            normalizedText = normalize(request.url(), extractedArticle.text());
            originalInput = sourceUrl;
            explicitVisitedUrls.add(sourceUrl);
            extraction = gradientAiClient.extractSignals(normalizedText, inputType, sourceUrl, sourceTitle, sourceLabel);
            aiAnalysis = gradientAiClient.analyzeInvestigation(normalizedText, inputType, sourceUrl, sourceTitle, sourceLabel, extraction);
        } else if (hasImage) {
            inputType = InputType.IMAGE;
            imageMimeType = normalizeImageMimeType(request.imageMimeType());
            String imageBase64 = sanitizeBase64(request.imageBase64());
            validateBase64(imageBase64);
            sourceTitle = hasValue(request.imageName()) ? request.imageName().trim() : "Uploaded image";
            sourceLabel = sourceTitle;
            originalInput = sourceTitle;
            extraction = gradientAiClient.extractImageSignals(imageBase64, imageMimeType, sourceTitle, sourceLabel);
            aiAnalysis = gradientAiClient.analyzeInvestigation(null, inputType, null, sourceTitle, sourceLabel, extraction);
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
            extraction = gradientAiClient.extractSignals(normalizedText, inputType, sourceUrl, sourceTitle, sourceLabel);
            aiAnalysis = gradientAiClient.analyzeInvestigation(normalizedText, inputType, sourceUrl, sourceTitle, sourceLabel, extraction);
        } else {
            inputType = InputType.TEXT;
            sourceLabel = "Manual text submission";
            normalizedText = normalize("text", request.text());
            originalInput = request.text().trim();
            extraction = gradientAiClient.extractSignals(normalizedText, inputType, null, null, sourceLabel);
            aiAnalysis = gradientAiClient.analyzeInvestigation(normalizedText, inputType, null, null, sourceLabel, extraction);
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

        AsiSignalExtractionResult extraction = gradientAiClient.extractSignals(
            normalizedText,
            inputType,
            null,
            sourceTitle,
            sourceLabel
        );
        GradientAiClient.AiAnalysis aiAnalysis = gradientAiClient.analyzeInvestigation(
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
        var followUp = gradientAiClient.followUp(record, question);

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
        GradientAiClient.AiAnalysis aiAnalysis,
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
        record.setModelUsed(defaultText(aiAnalysis.modelUsed(), "gradient-default"));
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
        record.setAdvancedModules(resolveAdvancedModules(
            result.advanced_modules(),
            inputType,
            resolution,
            normalizedText,
            record.getRiskLabels(),
            record.getVisitedUrls(),
            record.getRecommendedChecks()
        ));
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
                clampScoreOrDefault(claim == null ? null : claim.confidence(), 50),
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
            toResponseModules(record, resolution),
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
                record.getRecommendedChecks(),
                null
            )
        );

        AnalysisStatus status = record.getAnalysisStatus();
        String reason = record.getStatusReason();
        Integer score = clampScoreOrNull(record.getCredibilityScore());
        Verdict verdict = record.getVerdict();
        score = reconcileScoreWithVerdict(score, verdict);

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
                score = reconcileScoreWithVerdict(score, verdict);
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

    private Integer clampScoreOrNull(Integer score) {
        if (score == null) {
            return null;
        }
        return Math.max(0, Math.min(100, score));
    }

    private int clampScoreOrDefault(Integer score, int fallback) {
        Integer normalized = clampScoreOrNull(score);
        return normalized == null ? fallback : normalized;
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

    private AdvancedModulesInsight resolveAdvancedModules(
        AdvancedAnalysisModules modules,
        InputType inputType,
        AnalysisResolution resolution,
        String normalizedText,
        List<String> riskLabels,
        List<String> visitedUrls,
        List<String> recommendedChecks
    ) {
        String content = defaultText(normalizedText, "").toLowerCase();
        List<String> labels = cleanList(riskLabels);

        List<String> sharingFactors = cleanList(modules == null ? null : modules.sharingFactors());
        List<String> viralityReasons = cleanList(modules == null ? null : modules.viralityReasons());
        List<String> manipulationSignals = cleanList(modules == null ? null : modules.manipulationSignals());
        List<String> technicalRisks = cleanList(modules == null ? null : modules.technicalRisks());

        if (sharingFactors.isEmpty()) {
            if (content.length() > 0 && content.length() < 280) {
                sharingFactors = List.of("Message court et facile a repartager");
            }
            if (containsAny(content, "urgent", "breaking", "share now", "immediat", "alerte", "viral")) {
                sharingFactors = appendUnique(sharingFactors, "Ton sensationnel ou urgent");
            }
            if (containsAny(content, "free", "gratuit", "subsidy", "prime", "gagner", "cadeau")) {
                sharingFactors = appendUnique(sharingFactors, "Promesse forte qui incite au partage");
            }
        }

        if (manipulationSignals.isEmpty()) {
            if (containsAny(content, "urgent", "immediat", "right now", "maintenant")) {
                manipulationSignals = appendUnique(manipulationSignals, "Sentiment d'urgence");
            }
            if (containsAny(content, "fear", "danger", "panic", "peur", "menace")) {
                manipulationSignals = appendUnique(manipulationSignals, "Appel a la peur");
            }
            if (containsAny(content, "they hide", "complot", "cover-up", "cache la verite")) {
                manipulationSignals = appendUnique(manipulationSignals, "Rhetorique complotiste");
            }
            if (containsAny(content, "official experts say", "source officielle", "docteur", "scientists confirm")) {
                manipulationSignals = appendUnique(manipulationSignals, "Fausse autorite potentielle");
            }
            if (containsAny(content, "click", "cliquez", "must watch", "incroyable", "choc")) {
                manipulationSignals = appendUnique(manipulationSignals, "Langage clickbait");
            }
        }

        if (viralityReasons.isEmpty()) {
            if (!sharingFactors.isEmpty()) {
                viralityReasons = appendUnique(viralityReasons, "Le format est facilement partageable sur les reseaux sociaux");
            }
            if (!manipulationSignals.isEmpty()) {
                viralityReasons = appendUnique(viralityReasons, "La charge emotionnelle peut accelerer la diffusion");
            }
            if (labels.stream().anyMatch(label -> label.toLowerCase().contains("polit"))) {
                viralityReasons = appendUnique(viralityReasons, "Le sujet politique augmente la probabilite de diffusion");
            }
        }

        if (technicalRisks.isEmpty()) {
            boolean hasLink = (visitedUrls != null && !visitedUrls.isEmpty()) || containsAny(content, "http://", "https://", "www.");
            if (hasLink && containsAny(content, "login", "verify", "account", "otp", "bank", "wallet")) {
                technicalRisks = appendUnique(technicalRisks, "Risque potentiel de phishing");
            }
            if (hasLink && containsAny(content, "apk", "download", ".exe", "crack", "mod")) {
                technicalRisks = appendUnique(technicalRisks, "Risque potentiel de malware");
            }
            if (hasLink && containsAny(content, "watch free", "stream free", "telecharger film", "iptv")) {
                technicalRisks = appendUnique(technicalRisks, "Possible site de streaming illegal");
            }
            if (labels.stream().anyMatch(label -> label.toLowerCase().contains("scam"))) {
                technicalRisks = appendUnique(technicalRisks, "Risque d'arnaque");
            }
        }

        String viralityLevel = normalizeLevel(defaultText(modules == null ? null : modules.viralityLevel(), null));
        if (viralityLevel == null) {
            int viralityScore = sharingFactors.size() + manipulationSignals.size();
            viralityLevel = viralityScore >= 4 ? "Eleve" : viralityScore >= 2 ? "Moyen" : "Faible";
        }

        String impact = normalizeLevel(defaultText(modules == null ? null : modules.misinformationImpact(), null));
        if (impact == null) {
            impact = inferImpact(labels, resolution);
        }

        String trustRiskLevel = normalizeLevel(defaultText(modules == null ? null : modules.trustRiskLevel(), null));
        if (trustRiskLevel == null) {
            trustRiskLevel = inferTrustRiskLevel(resolution);
        }

        String trustConclusion = defaultText(modules == null ? null : modules.trustConclusion(), null);
        if (trustConclusion == null) {
            trustConclusion = switch (trustRiskLevel) {
                case "Eleve" -> "Contenu potentiellement trompeur ou faux";
                case "Moyen" -> "Contenu a verifier avant partage";
                default -> "Contenu plutot fiable, verification humaine recommandee pour les points sensibles";
            };
        }

        List<String> securityRecommendations = cleanList(modules == null ? null : modules.securityRecommendations());
        if (securityRecommendations.isEmpty()) {
            securityRecommendations = new ArrayList<>();
            if ("Eleve".equals(trustRiskLevel)) {
                securityRecommendations.add("Ne pas partager ce contenu en l'etat");
            }
            securityRecommendations.add("Verifier aupres de sources officielles");
            if (!technicalRisks.isEmpty()) {
                securityRecommendations.add("Ne pas cliquer sur les liens suspects sans verification");
            }
            securityRecommendations.add("Demander une verification humaine pour les affirmations critiques");
            securityRecommendations = List.copyOf(new LinkedHashSet<>(securityRecommendations));
        }

        List<String> humanVerificationSteps = cleanList(modules == null ? null : modules.humanVerificationSteps());
        if (humanVerificationSteps.isEmpty()) {
            humanVerificationSteps = cleanList(recommendedChecks);
        }

        String impactReason = defaultText(modules == null ? null : modules.impactReason(), null);
        if (impactReason == null) {
            impactReason = "Eleve".equals(impact)
                ? "Le contenu peut influencer des decisions sensibles s'il est partage sans verification."
                : "Moyen".equals(impact)
                    ? "Le contenu peut induire en erreur un public large selon son contexte de diffusion."
                    : "Le risque de dommage est limite mais une verification reste recommandee.";
        }

        AdvancedModulesInsight insight = new AdvancedModulesInsight();
        insight.setViralityLevel(viralityLevel);
        insight.setViralityReasons(viralityReasons);
        insight.setSharingFactors(sharingFactors);
        insight.setManipulationSignals(manipulationSignals);
        insight.setMisinformationImpact(impact);
        insight.setImpactReason(impactReason);
        insight.setTrustRiskLevel(trustRiskLevel);
        insight.setTrustConclusion(trustConclusion);
        insight.setTechnicalRisks(technicalRisks);
        insight.setSecurityRecommendations(securityRecommendations);
        insight.setHumanVerificationSteps(humanVerificationSteps);
        return insight;
    }

    private AdvancedAnalysisModules toResponseModules(AnalysisRecord record, AnalysisResolution resolution) {
        AdvancedModulesInsight modules = record.getAdvancedModules();
        if (modules == null) {
            modules = resolveAdvancedModules(
                null,
                record.getInputType(),
                resolution,
                record.getNormalizedText(),
                record.getRiskLabels(),
                record.getVisitedUrls(),
                record.getRecommendedChecks()
            );
        }

        return new AdvancedAnalysisModules(
            defaultText(modules.getViralityLevel(), "Moyen"),
            cleanList(modules.getViralityReasons()),
            cleanList(modules.getSharingFactors()),
            cleanList(modules.getManipulationSignals()),
            defaultText(modules.getMisinformationImpact(), "Moyen"),
            defaultText(modules.getImpactReason(), "Impact non determine avec precision."),
            defaultText(modules.getTrustRiskLevel(), inferTrustRiskLevel(resolution)),
            defaultText(modules.getTrustConclusion(), "Verification humaine recommandee."),
            cleanList(modules.getTechnicalRisks()),
            cleanList(modules.getSecurityRecommendations()),
            cleanList(modules.getHumanVerificationSteps())
        );
    }

    private String inferImpact(List<String> riskLabels, AnalysisResolution resolution) {
        List<String> labels = riskLabels == null ? List.of() : riskLabels.stream().map(String::toLowerCase).toList();
        if (labels.stream().anyMatch(label -> label.contains("public safety") || label.contains("health") || label.contains("financial") || label.contains("scam"))) {
            return "Eleve";
        }
        if (labels.stream().anyMatch(label -> label.contains("polit") || label.contains("misinformation") || label.contains("impersonation"))) {
            return "Moyen";
        }
        Integer score = resolution.score();
        if (score != null && score < 35) {
            return "Eleve";
        }
        return "Faible";
    }

    private String inferTrustRiskLevel(AnalysisResolution resolution) {
        if (resolution.status() == AnalysisStatus.CANNOT_ANALYZE) {
            return "Moyen";
        }
        Integer score = resolution.score();
        if (score == null) {
            return "Moyen";
        }
        if (score < 40) {
            return "Eleve";
        }
        if (score < 70) {
            return "Moyen";
        }
        return "Faible";
    }

    private String normalizeLevel(String level) {
        if (level == null) {
            return null;
        }
        String normalized = level.trim().toLowerCase();
        if (normalized.startsWith("elev") || normalized.equals("high")) {
            return "Eleve";
        }
        if (normalized.startsWith("moy") || normalized.equals("medium")) {
            return "Moyen";
        }
        if (normalized.startsWith("fai") || normalized.equals("low")) {
            return "Faible";
        }
        return null;
    }

    private boolean containsAny(String content, String... terms) {
        if (content == null || content.isBlank()) {
            return false;
        }
        for (String term : terms) {
            if (content.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private List<String> appendUnique(List<String> values, String item) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(values == null ? List.of() : values);
        merged.add(item);
        return List.copyOf(merged);
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
        Integer score = clampScoreOrNull(result.credibility_score());
        Verdict verdict = null;
        if (score != null) {
            verdict = Verdict.fromLabel(result.verdict());
            if (verdict == null) {
                verdict = Verdict.fromScore(score);
            }
        }
        score = reconcileScoreWithVerdict(score, verdict);

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

    private Integer reconcileScoreWithVerdict(Integer score, Verdict verdict) {
        if (score == null || verdict == null) {
            return score;
        }

        return switch (verdict) {
            case LIKELY_FAKE -> score <= 29 ? score : 18;
            case QUESTIONABLE -> (score >= 30 && score <= 59) ? score : 45;
            case LIKELY_TRUE -> (score >= 60 && score <= 79) ? score : 72;
            case VERIFIED -> score >= 80 ? score : 92;
        };
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
