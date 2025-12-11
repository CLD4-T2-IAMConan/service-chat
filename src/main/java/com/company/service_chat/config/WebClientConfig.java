package com.company.service_chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient ticketWebClient() {
        return WebClient.builder()
                .baseUrl("http://ticket-service:8080") // 티켓 서비스 URL
                .build();
    }
}
