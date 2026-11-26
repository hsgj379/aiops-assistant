package com.aiops.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型配置（阿里云百炼 OpenAI 兼容接口）。
 * 全部字段通过环境变量注入，缺失 API Key 时不阻止应用启动。
 */
@ConfigurationProperties(prefix = "aiops.ai")
public class AiProperties {

    /** Chat Completions 基础地址，例如 https://dashscope.aliyuncs.com/compatible-mode/v1 */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private String apiKey;

    private String model = "qwen-plus";

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 读取超时（毫秒） */
    private int readTimeout = 60000;

    /** 单次请求最大输出 Token，防止异常消耗 */
    private int maxOutputTokens = 1500;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
}
