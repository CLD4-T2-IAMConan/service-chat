// 채팅방 정보를 프론트로 내려줄 때 사용하는 응답용 DTO
// ChatRoom 엔티티의 요약 정보(?)를 클라이언트가 보기 좋은 형태로 정리한 DTO
// 채팅방 목록 조회 or 채팅방 상세 화면 입장 시 사용
package com.company.service_chat.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomResponse {
    private Long chatroomId;
    private Long ticketId;
    private LocalDateTime createdAt;
    private String roomStatus;

    // 채팅 목록에 표시할 최신 메시지 정보
    private String lastMessageContent;      // 마지막 메시지 내용
    private String lastMessageType;         // 마지막 메시지 타입 (TEXT, SYSTEM_ACTION_MESSAGE, SYSTEM_INFO_MESSAGE)
    private LocalDateTime lastMessageTime;  // 마지막 메시지 시간
    private Integer unreadCount;            // 안 읽은 메시지 수
}
