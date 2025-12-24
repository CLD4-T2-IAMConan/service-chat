package com.company.service_chat.service;

import com.company.service_chat.dto.ChatMessageDto;
import com.company.service_chat.dto.ChatMessageResponse;
import com.company.service_chat.entity.ChatMember;
import com.company.service_chat.entity.ChatMessage;
import com.company.service_chat.entity.ChatRoom;
import com.company.service_chat.repository.ChatMemberRepository;
import com.company.service_chat.repository.ChatMessageRepository;
import com.company.service_chat.repository.ChatRoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ObjectMapper objectMapper; // metadata 직렬화/역직렬화용

    // 생성자 주입(필드 추가 반영)
    public ChatService(ChatMessageRepository chatMessageRepository,
                       ChatRoomRepository chatRoomRepository,
                       ChatMemberRepository chatMemberRepository,
                       ObjectMapper objectMapper) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.objectMapper = objectMapper;
    }

    // --- 1. 일반 메시지 DB 저장 ---
    @Transactional
    public ChatMessage saveMessage(ChatMessageDto messageDto) {
        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getChatroomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        Long senderId = messageDto.getSenderId();

        // 2. 권한 체크
        boolean isParticipant =
                senderId.equals(chatRoom.getSellerId()) ||
                        senderId.equals(chatRoom.getBuyerId());

        if (!isParticipant) {
            throw new IllegalStateException("채팅방 참여자만 메시지 전송 가능");
        }
        // Enum 매핑
        ChatMessage.MessageType mappedType =
                ChatMessage.MessageType.valueOf(messageDto.getType().name());
        // Object -> JSON 문자열로 변환 (DTO에서 metadata Object로 저장했었음)
        String metadataJson = null;
        if (messageDto.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(messageDto.getMetadata());
            } catch (Exception e) {
                metadataJson = null;
            }
        }
        ChatMessage message = ChatMessage.builder()
                .chatroomId(messageDto.getChatroomId())
                .senderId(messageDto.getSenderId())
                .type(mappedType)
                .content(messageDto.getContent())
                .metadata(metadataJson) // JSON string 저장
                .build();
        message = chatMessageRepository.save(message);
        return message;
    }

    // REST / WS 모두 동일한 메시지 구조로 응답을 주기 위해 추가
    public ChatMessageResponse toResponse(ChatMessage message) {
        Object metadataObj = null;
        if (message.getMetadata() != null) {
            try {
                metadataObj = objectMapper.readValue(message.getMetadata(), Object.class);
            } catch (Exception e) {
                metadataObj = null;
            }
        }
        return ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSenderId())
                .type(message.getType().name())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .metadata(metadataObj)
                .build();
    }

    // --- 2. 시스템 메시지 저장 ---
    @Transactional
    public ChatMessage saveSystemMessage(Long chatroomId, Long senderId, ChatMessageDto.MessageType type, String content, Object metadata) {
        ChatMessageDto dto = ChatMessageDto.builder()
                .chatroomId(chatroomId)
                .senderId(senderId)
                .type(type)
                .content(content)
                .metadata(metadata)
                .build();
        return saveMessage(dto); // JSON 으로 변환
    }

    // --- 3. 특정 채팅방 메시지 목록 조회 (GET /chat/rooms/{id}/messages) ---
    public List<ChatMessageResponse> getMessagesByChatroomId(Long chatroomId) {
        // 1. Repository를 통해 메시지 목록 조회 (오래된 순)
        List<ChatMessage> messages = chatMessageRepository.findByChatroomIdOrderBySentAtAsc(chatroomId);

        // 2. Entity -> Response DTO 변환
        return messages.stream()
                .map(message -> {
                    Object metadataObj = null;

                    // DB에 String(JSON)으로 저장된 것을 다시 Object로 역직렬화
                    if (message.getMetadata() != null) {
                        try {
                            metadataObj =
                                    objectMapper.readValue(message.getMetadata(), Object.class);
                        } catch (Exception e) {
                            metadataObj = null;
                        }
                    }

                    return ChatMessageResponse.builder()
                            .messageId(message.getMessageId())
                            .senderId(message.getSenderId())
                            .type(message.getType().name())
                            .content(message.getContent())
                            .sentAt(message.getSentAt())
                            .metadata(metadataObj)   // 프론트는 Object로 받음
                            .build();
                })
                .collect(Collectors.toList());
    }

    //--- 4. 메시지 읽음 처리 ---
    @Transactional
    public void markAsRead(Long chatroomId, Long userId, Long lastReadMessageId) {

        ChatMember chatMember = chatMemberRepository
                .findByUserIdAndChatroomId(userId, chatroomId)
                .orElseThrow(() -> new NoSuchElementException("해당 채팅방의 멤버가 아닙니다."));

        // 더 큰 값으로만 업데이트 (뒤로 돌아가면 안됨)
        if (lastReadMessageId > chatMember.getLastReadMessageId()) {
            chatMember.updateLastReadMessageId(lastReadMessageId);
        }
    }
}