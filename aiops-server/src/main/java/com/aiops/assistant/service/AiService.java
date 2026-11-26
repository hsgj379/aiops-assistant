package com.aiops.assistant.service;

import com.aiops.assistant.dto.AiChatResponse;
import com.aiops.assistant.dto.AiDiagnoseRequest;
import com.aiops.assistant.dto.AiDiagnoseResponse;

public interface AiService {

    /**
     * 运维问答。
     *
     * @param question 用户问题
     * @param userId   当前登录用户 ID（仅用于日志追踪）
     */
    AiChatResponse chat(String question, Long userId);

    /**
     * 故障诊断，返回结构化结果。
     *
     * @param request 告警现场信息
     * @param userId  当前登录用户 ID（仅用于日志追踪）
     */
    AiDiagnoseResponse diagnose(AiDiagnoseRequest request, Long userId);
}
