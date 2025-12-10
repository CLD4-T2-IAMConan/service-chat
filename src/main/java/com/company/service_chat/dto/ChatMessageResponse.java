// 클라이언트(프론트)로 반환할 때 사용하는 응답용 DTO
package com.company.service_chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {

    private Long messageId;
    private Long senderId;
    private String type;
    private String content;
    private LocalDateTime sentAt;
    private Object metadata; // 필요 시에만
}
