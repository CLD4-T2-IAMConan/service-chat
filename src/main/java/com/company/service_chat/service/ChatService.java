package com.company.service_chat.service;

import com.company.service_chat.dto.ChatMessageDto;
import com.company.service_chat.dto.ChatMessageResponse;
import com.company.service_chat.entity.ChatMessage;
import com.company.service_chat.entity.ChatRoom;
import com.company.service_chat.repository.ChatMessageRepository;
import com.company.service_chat.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ChatService(ChatMessageRepository chatMessageRepository,
                       ChatRoomRepository chatRoomRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomRepository = chatRoomRepository;
    }

    // --- 1. 일반 메시지 및 시스템 메시지 DB 저장 ---
    @Transactional
    public ChatMessage saveMessage(ChatMessageDto messageDto) {

        // 1. DTO -> Entity 변환
        ChatMessage message = ChatMessage.builder()
                .chatroomId(messageDto.getChatroomId())
                .senderId(messageDto.getSenderId())
                .type(ChatMessage.MessageType.valueOf(messageDto.getType().name())) // Enum 매핑
                .content(messageDto.getContent())
                // .metadata는 JSON 직렬화하여 String으로 저장할 수 있습니다.
                .build();

        // 2. 메시지 저장
        message = chatMessageRepository.save(message);

        // 3. ChatRoom의 lastMessageId 업데이트 (Optional)
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(messageDto.getChatroomId());
        chatRoomOpt.ifPresent(room -> {
            // lastMessageId는 Setter 없이 update 메서드를 ChatRoom Entity에 구현하는 것이 좋습니다.
            // room.updateLastMessageId(message.getMessageId());
            // chatRoomRepository.save(room); // 변경 감지
        });

        return message;
    }

    // --- 2. 시스템 메시지 저장 (ChatRoomService에서 사용) ---
    @Transactional
    public ChatMessage saveSystemMessage(Long chatroomId, Long senderId,
                                         ChatMessageDto.MessageType type, String content, Object metadata) {
        ChatMessageDto dto = ChatMessageDto.builder()
                .chatroomId(chatroomId)
                .senderId(senderId) // 시스템 메시지이지만, 누가 요청했는지 senderId를 기록
                .type(type)
                .content(content)
                .build();
        return saveMessage(dto);
    }

    // --- 3. 특정 채팅방 메시지 목록 조회 (GET /chat/rooms/{id}/messages) ---
    public List<ChatMessageResponse> getMessagesByChatroomId(Long chatroomId) {
        // 1. Repository를 통해 메시지 목록 조회 (오래된 순)
        List<ChatMessage> messages = chatMessageRepository.findByChatroomIdOrderBySentAtAsc(chatroomId);

        // 2. Entity -> Response DTO 변환
        return messages.stream()
                .map(message -> ChatMessageResponse.builder()
                        .messageId(message.getMessageId())
                        .senderId(message.getSenderId())
                        .type(message.getType().name())
                        .content(message.getContent())
                        .sentAt(message.getSentAt())
                        .build())
                .collect(Collectors.toList());
    }

    // --- 4. 메시지 읽음 처리 (선택 사항) ---
//    @Transactional
//    public void markAsRead(Long chatroomId, Long userId, Long lastReadMessageId) {
//        chatMemberRepository.findByChatroomIdAndUserId(chatroomId, userId)
//                .ifPresent(member -> {
//                    // ChatMember Entity의 updateLastReadMessageId 메서드를 호출
//                    member.updateLastReadMessageId(lastReadMessageId);
//                });
//    }
}