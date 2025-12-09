package com.company.service_chat.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatroomId; // PK
    private Long ticketId;
    private Long buyerId;
    private Long lastMessageId;

    @Enumerated(EnumType.STRING) // DB에 ENUM 이름을 문자열로 저장
    private RoomStatus roomStatus;

    @Enumerated(EnumType.STRING)
    private DealStatus dealStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // DB의 ENUM 타입과 일치시켜야 함
    public enum RoomStatus { OPEN, LOCK }
    public enum DealStatus { PENDING, REQUESTED, ACCEPTED, REJECTED, COMPLETED }

    @PrePersist
    public void prePersist() { // 저장될 때 디폴트값 설정
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.roomStatus = RoomStatus.OPEN;
        this.dealStatus = DealStatus.PENDING;
    }

    @Builder
    public ChatRoom(Long ticketId, Long buyerId) {
        this.ticketId = ticketId;
        this.buyerId = buyerId;
    }

    // 상태 변경 메서드 (비즈니스 로직)
    public void updateStatus(RoomStatus roomStatus, DealStatus dealStatus) {
        this.roomStatus = roomStatus;
        this.dealStatus = dealStatus;
        this.updatedAt = LocalDateTime.now();
    }
}