package com.aiops.assistant.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 故障诊断请求。仅 serviceName、alertTitle 必填，其余可选。
 */
public class AiDiagnoseRequest {

    @NotBlank(message = "服务名称不能为空")
    @Size(max = 128, message = "服务名称长度不能超过 128")
    private String serviceName;

    @NotBlank(message = "告警标题不能为空")
    @Size(max = 256, message = "告警标题长度不能超过 256")
    private String alertTitle;

    @Size(max = 32, message = "告警级别长度不能超过 32")
    private String alertLevel;

    @Size(max = 2000, message = "告警描述长度不能超过 2000")
    private String alertDescription;

    @Size(max = 8000, message = "日志长度不能超过 8000")
    private String logs;

    @Size(max = 4000, message = "指标信息长度不能超过 4000")
    private String metrics;

    @Size(max = 64, message = "运行环境长度不能超过 64")
    private String environment;

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getAlertTitle() { return alertTitle; }
    public void setAlertTitle(String alertTitle) { this.alertTitle = alertTitle; }
    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }
    public String getAlertDescription() { return alertDescription; }
    public void setAlertDescription(String alertDescription) { this.alertDescription = alertDescription; }
    public String getLogs() { return logs; }
    public void setLogs(String logs) { this.logs = logs; }
    public String getMetrics() { return metrics; }
    public void setMetrics(String metrics) { this.metrics = metrics; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
}
