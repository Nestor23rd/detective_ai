package com.aidetective.backend.analysis.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "analysis_history")
public class AnalysisRecord {

    @Id
    private String id;

    private InputType inputType;
    private String originalInput;

    @Indexed
    private String sourceUrl;

    private String normalizedText;
    private String sourceTitle;
    private String sourceLabel;
    private String imageMimeType;
    private String modelUsed;
    private AnalysisStatus analysisStatus = AnalysisStatus.COMPLETED;
    private String statusReason;
    private String contentLanguage;
    private List<String> riskLabels = new ArrayList<>();
    private List<String> visitedUrls = new ArrayList<>();
    private String summary;
    private List<ClaimInsight> claims = new ArrayList<>();
    private List<EntityInsight> entities = new ArrayList<>();
    private Integer credibilityScore;
    private Verdict verdict;
    private String reasoning;
    private List<String> limitations = new ArrayList<>();
    private List<String> recommendedChecks = new ArrayList<>();
    private AdvancedModulesInsight advancedModules;
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public String getOriginalInput() {
        return originalInput;
    }

    public void setOriginalInput(String originalInput) {
        this.originalInput = originalInput;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public void setNormalizedText(String normalizedText) {
        this.normalizedText = normalizedText;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String getImageMimeType() {
        return imageMimeType;
    }

    public void setImageMimeType(String imageMimeType) {
        this.imageMimeType = imageMimeType;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public AnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(AnalysisStatus analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getContentLanguage() {
        return contentLanguage;
    }

    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    public List<String> getRiskLabels() {
        return riskLabels;
    }

    public void setRiskLabels(List<String> riskLabels) {
        this.riskLabels = riskLabels;
    }

    public List<String> getVisitedUrls() {
        return visitedUrls;
    }

    public void setVisitedUrls(List<String> visitedUrls) {
        this.visitedUrls = visitedUrls;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ClaimInsight> getClaims() {
        return claims;
    }

    public void setClaims(List<ClaimInsight> claims) {
        this.claims = claims;
    }

    public List<EntityInsight> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityInsight> entities) {
        this.entities = entities;
    }

    public Integer getCredibilityScore() {
        return credibilityScore;
    }

    public void setCredibilityScore(Integer credibilityScore) {
        this.credibilityScore = credibilityScore;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<String> getLimitations() {
        return limitations;
    }

    public void setLimitations(List<String> limitations) {
        this.limitations = limitations;
    }

    public List<String> getRecommendedChecks() {
        return recommendedChecks;
    }

    public void setRecommendedChecks(List<String> recommendedChecks) {
        this.recommendedChecks = recommendedChecks;
    }

    public AdvancedModulesInsight getAdvancedModules() {
        return advancedModules;
    }

    public void setAdvancedModules(AdvancedModulesInsight advancedModules) {
        this.advancedModules = advancedModules;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
