package com.aidetective.backend.analysis.dto;

import java.util.List;

public record AdvancedAnalysisModules(
    String viralityLevel,
    List<String> viralityReasons,
    List<String> sharingFactors,
    List<String> manipulationSignals,
    String misinformationImpact,
    String impactReason,
    String trustRiskLevel,
    String trustConclusion,
    List<String> technicalRisks,
    List<String> securityRecommendations,
    List<String> humanVerificationSteps
) {
}

