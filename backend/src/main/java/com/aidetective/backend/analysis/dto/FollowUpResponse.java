package com.aidetective.backend.analysis.dto;

import java.util.List;

public record FollowUpResponse(
    String answer,
    List<String> sourceUrls,
    List<String> suggestedChecks
) {
}
