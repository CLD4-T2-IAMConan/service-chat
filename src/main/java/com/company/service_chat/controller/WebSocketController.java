package com.company.service_chat.controller;

import com.company.service_chat.dto.ChatMessageDto;
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

    /**
     * 클라이언트 → 서버
     * /pub/chat/message 로 메시지를 보내면 이 메서드가 작동
     */
    @MessageMapping("/chat/message")
    public void message(@Payload ChatMessageDto message) {

        // 1. 메시지 저장, 메시지 TYPE 신경 안 쓰고 저장
        chatService.saveMessage(message);

        // 2. 목적지 채팅방 설정
        String destination = "/topic/chatrooms/" + message.getChatroomId();

        // 3. 해당 채팅방을 구독하는 모든 사용자에게 메시지 전송
        messagingTemplate.convertAndSend(destination, message);
    }

    // 서버 -> 웹소켓 (프론트로 전달)
    @MessageMapping("/chat/{chatroomId}/system")
    public void systemMessage(
            @DestinationVariable Long chatroomId,
            @Payload ChatMessageDto message
    ) {
        // 1) 시스템 메시지 저장
        ChatMessage saved = chatService.saveSystemMessage(
                chatroomId,
                message.getSenderId(),
                message.getType(),     // ChatMessageDto.MessageType
                message.getContent(),  // 문자열 content
                message.getMetadata()  // Object metadata
        );

        // 2) 저장한 메시지를 다시 broadcast
        messagingTemplate.convertAndSend(
                "/topic/chatrooms/" + chatroomId,
                saved
        );
    }

}
