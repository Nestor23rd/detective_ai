package com.aidetective.backend.analysis.dto;

import java.util.List;

public record AsiInvestigationResult(
    String summary,
    List<AsiClaim> claims,
    List<AsiEntity> entities,
    String content_language,
    List<String> risk_labels,
    List<String> visited_urls,
    Integer credibility_score,
    String verdict,
    String reasoning,
    List<String> limitations,
    List<String> recommended_checks
) {
    public record AsiClaim(
        String statement,
        String suspicion,
        String evidence_hint,
        String assessment,
        Integer confidence,
        List<String> evidence_points,
        List<String> source_urls,
        String next_step
    ) {
    }

    public record AsiEntity(String name, String type, String context) {
    }
}
