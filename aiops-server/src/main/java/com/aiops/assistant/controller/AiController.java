package com.aiops.assistant.controller;

import com.aiops.assistant.common.Result;
import com.aiops.assistant.dto.AiChatRequest;
import com.aiops.assistant.dto.AiChatResponse;
import com.aiops.assistant.dto.AiDiagnoseRequest;
import com.aiops.assistant.dto.AiDiagnoseResponse;
import com.aiops.assistant.service.AiService;
import com.aiops.assistant.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * AI 接口：登录后可访问，用户身份只取自已认证上下文，不接受客户端传入 userId。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final AuthService authService;

    public AiController(AiService aiService, AuthService authService) {
        this.aiService = aiService;
        this.authService = authService;
    }

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request,
                                       Authentication authentication) {
        return Result.ok(aiService.chat(request.getQuestion(), currentUserId(authentication)));
    }

    @PostMapping("/diagnose")
    public Result<AiDiagnoseResponse> diagnose(@Valid @RequestBody AiDiagnoseRequest request,
                                               Authentication authentication) {
        return Result.ok(aiService.diagnose(request, currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        return authService.currentUser(authentication.getName()).getId();
    }
}
