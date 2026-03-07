package com.aidetective.backend.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FollowUpQuestionRequest(
    @NotBlank(message = "Please provide a follow-up question.")
    @Size(max = 400, message = "Follow-up questions are limited to 400 characters")
    String question
) {
}
