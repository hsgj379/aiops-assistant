package com.aiops.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HealthService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${spring.application.name}")
    private String serviceName;

    /**
     * 只检查 Java 应用自身存活状态，不混入 MySQL / Redis / RabbitMQ 等中间件连接检查。
     * 中间件健康通过独立端点 /actuator/health/db、/actuator/health/redis 等按需暴露。
     */
    public Map<String, Object> healthInfo() {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("status", "UP");
        info.put("service", serviceName);
        info.put("time", LocalDateTime.now().format(FMT));
        return info;
    }
}
