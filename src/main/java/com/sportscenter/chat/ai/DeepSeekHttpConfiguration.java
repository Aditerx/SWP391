package com.sportscenter.chat.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "scms.ai.chat.provider", havingValue = "deepseek")
@ConditionalOnExpression("'${scms.ai.chat.deepseek-api-key:}'.trim().length() > 0")
public class DeepSeekHttpConfiguration {
    @Bean
    public ClientHttpRequestFactory deepseekRequestFactory(ChatAiProperties properties) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(properties.getDeepseekTimeoutSeconds())).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(properties.getDeepseekTimeoutSeconds()));
        return factory;
    }
}
