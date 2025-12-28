package com.company.service_chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SystemActionRequest {
    private Long chatroomId;
    private String actionCode;
}
