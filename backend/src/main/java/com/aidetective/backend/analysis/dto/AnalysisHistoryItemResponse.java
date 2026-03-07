package com.aidetective.backend.analysis.dto;

import com.aidetective.backend.analysis.model.InputType;
import com.aidetective.backend.analysis.model.AnalysisStatus;
import java.time.Instant;

public record AnalysisHistoryItemResponse(
    String id,
    InputType inputType,
    String sourceUrl,
    String sourceTitle,
    String sourceLabel,
    String modelUsed,
    AnalysisStatus analysisStatus,
    String statusReason,
    String contentLanguage,
    java.util.List<String> riskLabels,
    String summary,
    Integer credibilityScore,
    String verdict,
    Instant createdAt
) {
}
