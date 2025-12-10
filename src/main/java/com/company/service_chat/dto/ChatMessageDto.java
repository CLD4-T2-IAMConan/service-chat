// 시스템 메시지 생성을 위함
package com.company.service_chat.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageDto {

    private Long messageId;
    private Long chatroomId;
    private Long senderId;
    private MessageType type;
    private String content;
    private String metadata;
    private LocalDateTime sentAt;

    public enum MessageType {
        TEXT,
        SYSTEM_ACTION_MESSAGE,
        SYSTEM_INFO_MESSAGE
    }
}
