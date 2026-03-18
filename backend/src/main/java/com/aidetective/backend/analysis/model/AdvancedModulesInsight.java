package com.aidetective.backend.analysis.model;

import java.util.ArrayList;
import java.util.List;

public class AdvancedModulesInsight {

    private String viralityLevel;
    private List<String> viralityReasons = new ArrayList<>();
    private List<String> sharingFactors = new ArrayList<>();
    private List<String> manipulationSignals = new ArrayList<>();
    private String misinformationImpact;
    private String impactReason;
    private String trustRiskLevel;
    private String trustConclusion;
    private List<String> technicalRisks = new ArrayList<>();
    private List<String> securityRecommendations = new ArrayList<>();
    private List<String> humanVerificationSteps = new ArrayList<>();

    public String getViralityLevel() {
        return viralityLevel;
    }

    public void setViralityLevel(String viralityLevel) {
        this.viralityLevel = viralityLevel;
    }

    public List<String> getViralityReasons() {
        return viralityReasons;
    }

    public void setViralityReasons(List<String> viralityReasons) {
        this.viralityReasons = viralityReasons;
    }

    public List<String> getSharingFactors() {
        return sharingFactors;
    }

    public void setSharingFactors(List<String> sharingFactors) {
        this.sharingFactors = sharingFactors;
    }

    public List<String> getManipulationSignals() {
        return manipulationSignals;
    }

    public void setManipulationSignals(List<String> manipulationSignals) {
        this.manipulationSignals = manipulationSignals;
    }

    public String getMisinformationImpact() {
        return misinformationImpact;
    }

    public void setMisinformationImpact(String misinformationImpact) {
        this.misinformationImpact = misinformationImpact;
    }

    public String getImpactReason() {
        return impactReason;
    }

    public void setImpactReason(String impactReason) {
        this.impactReason = impactReason;
    }

    public String getTrustRiskLevel() {
        return trustRiskLevel;
    }

    public void setTrustRiskLevel(String trustRiskLevel) {
        this.trustRiskLevel = trustRiskLevel;
    }

    public String getTrustConclusion() {
        return trustConclusion;
    }

    public void setTrustConclusion(String trustConclusion) {
        this.trustConclusion = trustConclusion;
    }

    public List<String> getTechnicalRisks() {
        return technicalRisks;
    }

    public void setTechnicalRisks(List<String> technicalRisks) {
        this.technicalRisks = technicalRisks;
    }

    public List<String> getSecurityRecommendations() {
        return securityRecommendations;
    }

    public void setSecurityRecommendations(List<String> securityRecommendations) {
        this.securityRecommendations = securityRecommendations;
    }

    public List<String> getHumanVerificationSteps() {
        return humanVerificationSteps;
    }

    public void setHumanVerificationSteps(List<String> humanVerificationSteps) {
        this.humanVerificationSteps = humanVerificationSteps;
    }
}

