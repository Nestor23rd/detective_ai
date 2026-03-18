package com.aidetective.backend.analysis.dto;

import com.aidetective.backend.analysis.model.InputType;
import com.aidetective.backend.analysis.model.AnalysisStatus;
import java.time.Instant;
import java.util.List;

public record AnalysisResponse(
    String id,
    InputType inputType,
    String sourceUrl,
    String sourceTitle,
    String sourceLabel,
    String imageMimeType,
    String modelUsed,
    AnalysisStatus analysisStatus,
    String statusReason,
    String contentLanguage,
    List<String> riskLabels,
    List<String> visitedUrls,
    String summary,
    List<ClaimResponse> claims,
    List<EntityResponse> entities,
    Integer credibilityScore,
    String verdict,
    String reasoning,
    List<String> limitations,
    List<String> recommendedChecks,
    AdvancedAnalysisModules advancedModules,
    Instant createdAt
) {
}
