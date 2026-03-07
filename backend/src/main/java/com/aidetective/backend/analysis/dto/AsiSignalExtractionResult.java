package com.aidetective.backend.analysis.dto;

import java.util.List;

public record AsiSignalExtractionResult(
    String summary,
    List<AsiClaimCandidate> claims,
    List<AsiEntity> entities,
    String content_language,
    List<String> risk_labels
) {
    public record AsiClaimCandidate(String statement, String why_it_matters) {
    }

    public record AsiEntity(String name, String type, String context) {
    }
}
