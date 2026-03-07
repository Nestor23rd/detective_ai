package com.aidetective.backend.analysis.dto;

public record ClaimResponse(
    String statement,
    String suspicion,
    String evidenceHint,
    String assessment,
    int confidence,
    java.util.List<String> evidencePoints,
    java.util.List<String> sourceUrls,
    String nextStep
) {
}
