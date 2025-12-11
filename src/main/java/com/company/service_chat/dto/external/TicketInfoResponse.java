// 채팅방 생성 시 sellerId를 티켓 서비스로부터 가져오기 위함

package com.company.service_chat.dto.external;

import lombok.Getter;

@Getter
public class TicketInfoResponse {
    private Long ticketId;
    private Long sellerId;
}
