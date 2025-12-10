package com.company.service_chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 1. 웹소켓 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 2. STOMP 엔드포인트(최초 연결 지점) 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 웹소켓 연결을 요청할 주소 (Handshake URL)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 문제 방지, 모든 도메인 허용
                .withSockJS(); // SockJS 지원 활성화 (하위 브라우저 호환성 확보)
    }

    // 3. 메시지 브로커 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트 -> 서버로 메시지를 보낼 때 사용하는 접두사 (Controller 라우팅)
        registry.setApplicationDestinationPrefixes("/app");

        // 서버 -> 클라이언트로 메시지를 보낼 때 사용하는 접두사
        // /topic: 다수에게 브로드캐스팅할 때 사용 (채팅방 메시지)
        // /queue: 특정 사용자에게 1:1로 보낼 때 사용 (개인 알림 등)
        registry.enableSimpleBroker("/topic", "/queue");
        // topic만 해도 될 것 같긴 한데.. 혹시 모르니 queue도 남겨둠
    }
}