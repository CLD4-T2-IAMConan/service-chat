// 채팅방 생성 시 sellerId를 티켓 서비스로부터 가져오기 위함

package com.company.service_chat.dto.external;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketInfoResponse {
    private Long ticketId;
    private Long ownerId; // TicketResponse의 ownerId를 sellerId로 사용
    private Long sellerId; // 호환성을 위해 유지
    
    // ownerId를 sellerId로 매핑하는 메서드
    public Long getSellerId() {
        return sellerId != null ? sellerId : ownerId;
    }
}
