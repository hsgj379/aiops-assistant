package com.aiops.assistant.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AiChatRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过 2000 个字符")
    private String question;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
