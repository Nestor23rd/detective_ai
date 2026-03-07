package com.aidetective.backend.analysis.model;

import java.util.ArrayList;
import java.util.List;

public class ClaimInsight {

    private String statement;
    private String suspicion;
    private String evidenceHint;
    private String assessment;
    private int confidence;
    private List<String> evidencePoints = new ArrayList<>();
    private List<String> sourceUrls = new ArrayList<>();
    private String nextStep;

    public ClaimInsight() {
    }

    public ClaimInsight(
        String statement,
        String suspicion,
        String evidenceHint,
        String assessment,
        int confidence,
        List<String> evidencePoints,
        List<String> sourceUrls,
        String nextStep
    ) {
        this.statement = statement;
        this.suspicion = suspicion;
        this.evidenceHint = evidenceHint;
        this.assessment = assessment;
        this.confidence = confidence;
        this.evidencePoints = evidencePoints == null ? new ArrayList<>() : new ArrayList<>(evidencePoints);
        this.sourceUrls = sourceUrls == null ? new ArrayList<>() : new ArrayList<>(sourceUrls);
        this.nextStep = nextStep;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getSuspicion() {
        return suspicion;
    }

    public void setSuspicion(String suspicion) {
        this.suspicion = suspicion;
    }

    public String getEvidenceHint() {
        return evidenceHint;
    }

    public void setEvidenceHint(String evidenceHint) {
        this.evidenceHint = evidenceHint;
    }

    public String getAssessment() {
        return assessment;
    }

    public void setAssessment(String assessment) {
        this.assessment = assessment;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public List<String> getEvidencePoints() {
        return evidencePoints;
    }

    public void setEvidencePoints(List<String> evidencePoints) {
        this.evidencePoints = evidencePoints;
    }

    public List<String> getSourceUrls() {
        return sourceUrls;
    }

    public void setSourceUrls(List<String> sourceUrls) {
        this.sourceUrls = sourceUrls;
    }

    public String getNextStep() {
        return nextStep;
    }

    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }
}
