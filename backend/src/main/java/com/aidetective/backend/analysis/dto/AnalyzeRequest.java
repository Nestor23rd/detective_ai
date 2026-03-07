package com.aidetective.backend.analysis.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnalyzeRequest(
    @Size(max = 12000, message = "Text input is limited to 12,000 characters")
    String text,
    String url,
    @Size(max = 8_000_000, message = "Image payload is too large")
    String imageBase64,
    @Pattern(regexp = "^image/(png|jpeg|jpg|webp|gif)$", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Only PNG, JPEG, WEBP, and GIF images are supported")
    String imageMimeType,
    @Size(max = 260, message = "Image name is too long")
    String imageName,
    String videoUrl
) {
}
