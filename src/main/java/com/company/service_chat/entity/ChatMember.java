package com.company.service_chat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_members")
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK
    private Long userId;     // FK (user_id)
    private Long chatroomId; // FK (chatroom_id)
    private Long lastReadMessageId; // 마지막으로 읽은 메시지의 ID
    private boolean isDeleted;      // 채팅방 삭제(숨김) 여부

    @Builder
    public ChatMember(Long userId, Long chatroomId) {
        this.userId = userId;
        this.chatroomId = chatroomId;
        this.isDeleted = false;
        this.lastReadMessageId = 0L; // 초기에는 0으로 설정
    }

    // 읽음 처리 업데이트 메서드
    public void updateLastReadMessageId(Long messageId) {
        this.lastReadMessageId = messageId;
    }

    // 채팅방 나가기(숨김) 처리 메서드
    public void markAsDeleted() {
        this.isDeleted = true;
    }
    
    // 채팅방 복구(삭제 해제) 처리 메서드
    public void restore() {
        this.isDeleted = false;
    }
}