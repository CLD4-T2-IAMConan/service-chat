package com.company.service_chat.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId; // PK
    private Long chatroomId; // FK
    private Long senderId; // FK

    @Enumerated(EnumType.STRING)
    private MessageType type; // 메시지 타입 (일반 메시지(TEXT), 시스템 메시지)

    @Column(columnDefinition = "TEXT") // TEXT 타입으로 매핑
    private String content;
    private LocalDateTime sentAt; // 메시지 전송 시각

    // JSON 타입 (DB에 따라 String으로 저장 후 JSON 파싱 필요)
    @Column(columnDefinition = "JSON")
    private String metadata; // 양도 수락/거절 같은 액션 버튼 정보 담는 거

    // MessageType ENUM 정의
    public enum MessageType {
        TEXT,
        SYSTEM_ACTION_MESSAGE,
        SYSTEM_INFO_MESSAGE
    }

    @PrePersist
    public void prePersist() { // 디폴트 값 설정
        this.sentAt = LocalDateTime.now();
    }

    @Builder
    public ChatMessage(Long messageId, Long chatroomId, Long senderId, MessageType type, String content, String metadata, LocalDateTime sentAt) {
        this.messageId = messageId; //
        this.chatroomId = chatroomId;
        this.senderId = senderId;
        this.type = type;
        this.sentAt = sentAt; //
        this.content = content;
        this.metadata = metadata;
    }
}