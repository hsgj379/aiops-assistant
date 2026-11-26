package com.aiops.assistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构化诊断结果。字段来自模型 JSON，服务端补齐 model / elapsedMs / confidenceNote。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiDiagnoseResponse {

    private String summary;
    private String severity;
    private List<String> possibleCauses = new ArrayList<String>();
    private List<String> troubleshootingSteps = new ArrayList<String>();
    private List<String> recommendations = new ArrayList<String>();
    private Double confidence;
    private List<String> evidence = new ArrayList<String>();

    /** 说明 confidence 为模型主观估计，非统计校准概率 */
    private String confidenceNote;
    private String model;
    private long elapsedMs;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public List<String> getPossibleCauses() { return possibleCauses; }
    public void setPossibleCauses(List<String> possibleCauses) { this.possibleCauses = possibleCauses; }
    public List<String> getTroubleshootingSteps() { return troubleshootingSteps; }
    public void setTroubleshootingSteps(List<String> troubleshootingSteps) { this.troubleshootingSteps = troubleshootingSteps; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
    public String getConfidenceNote() { return confidenceNote; }
    public void setConfidenceNote(String confidenceNote) { this.confidenceNote = confidenceNote; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
}
