package com.aiops.assistant.controller;

import com.aiops.assistant.common.Result;
import com.aiops.assistant.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private HealthService healthService;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(healthService.healthInfo());
    }
}
