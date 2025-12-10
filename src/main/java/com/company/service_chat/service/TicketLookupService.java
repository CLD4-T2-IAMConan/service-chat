package com.company.service_chat.service;

import com.company.service_chat.dto.external.TicketInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class TicketLookupService {

    private final WebClient ticketWebClient;

    public Long getSellerId(Long ticketId) {

        TicketInfoResponse ticketInfo = ticketWebClient.get()
                .uri("/tickets/{ticketId}", ticketId)
                .retrieve()
                .bodyToMono(TicketInfoResponse.class)
                .block(); // 동기 처리 (채팅방 생성 시 충분히 OK)

        if (ticketInfo == null) {
            throw new IllegalStateException("티켓 정보를 조회할 수 없습니다.");
        }

        return ticketInfo.getSellerId();
    }
}
