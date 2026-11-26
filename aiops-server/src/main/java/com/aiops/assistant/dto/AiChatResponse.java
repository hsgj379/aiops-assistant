package com.aiops.assistant.dto;

public class AiChatResponse {

    private String answer;

    private String model;

    /** 本次模型调用耗时（毫秒） */
    private long elapsedMs;

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
}
