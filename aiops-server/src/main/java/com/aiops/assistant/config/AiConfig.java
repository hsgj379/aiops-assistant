package com.aiops.assistant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /**
     * 专用 RestTemplate：连接/读取超时可配置；不默认抛错，
     * 由 AiServiceImpl 统一按 HTTP 状态码映射业务异常。
     */
    @Bean
    public RestTemplate aiRestTemplate(AiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) {
                // 交由业务层统一处理
            }
        });
        return restTemplate;
    }
}
