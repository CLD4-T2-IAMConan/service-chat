package com.company.service_chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient ticketWebClient() {
        return WebClient.builder()
                .baseUrl("http://host.docker.internal:8082") // 티켓 서비스 URL (호스트에서 실행 중)
                .build();
    }
}
