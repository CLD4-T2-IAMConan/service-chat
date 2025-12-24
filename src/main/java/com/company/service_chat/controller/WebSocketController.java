package com.company.service_chat.controller;

import com.company.service_chat.dto.ChatMessageDto;
import com.company.service_chat.dto.ChatMessageResponse;
import com.company.service_chat.entity.ChatMessage;
import com.company.service_chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    // 클라이언트 → 서버
    // /pub/chat/message 로 메시지를 보내면 이 메서드가 작동
    @MessageMapping("/chat/message")
    public void message(@Payload ChatMessageDto dto) {

        // 1. DB에 메시지 저장
        ChatMessage saved = chatService.saveMessage(dto);

        // 2. 응답 구조 바꿔주기 (REST와 WS을 동일하게)
        ChatMessageResponse response = chatService.toResponse(saved);

        // 3. 목적지 채팅방 설정
        String destination = "/topic/chatrooms/" + saved.getChatroomId();

        // 4. WebSocket으로 전송
        messagingTemplate.convertAndSend(destination, response);
    }

    // 서버 -> 웹소켓 (프론트로 전달)
    @MessageMapping("/chat/{chatroomId}/system")
    public void systemMessage(
            @DestinationVariable Long chatroomId,
            @Payload ChatMessageDto message
    ) {
        // 1. 시스템 메시지 저장
        ChatMessage saved = chatService.saveSystemMessage(
                chatroomId,
                message.getSenderId(),
                message.getType(),     // ChatMessageDto.MessageType
                message.getContent(),  // 문자열 content
                message.getMetadata()  // Object metadata
        );
        // 2. 시스템 메시지 쪽도 응답 구조 바꿔주기
        ChatMessageResponse response = chatService.toResponse(saved);
        // 3. 저장한 메시지를 다시 broadcast
        messagingTemplate.convertAndSend(
                "/topic/chatrooms/" + chatroomId,
                response
        );
    }

}
