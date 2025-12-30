package com.company.service_chat.service;

import com.company.service_chat.dto.ApiResponse;
import com.company.service_chat.dto.external.TicketInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketLookupService {

    private final WebClient ticketWebClient;
    private final ObjectMapper objectMapper;

    public Long getSellerId(Long ticketId) {
        try {
            // 티켓 서비스는 ApiResponse<TicketResponse> 형태로 응답
            JsonNode response = ticketWebClient.get()
                    .uri("/api/tickets/{ticketId}", ticketId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                log.error("티켓 정보 조회 실패: ticketId={}, 응답이 null입니다.", ticketId);
                throw new IllegalStateException("티켓 정보를 조회할 수 없습니다.");
            }

            // ApiResponse 구조에서 data 필드 추출
            JsonNode dataNode = response.get("data");
            if (dataNode == null || dataNode.isNull()) {
                log.error("티켓 정보 조회 실패: ticketId={}, data 필드가 null입니다.", ticketId);
                throw new IllegalStateException("티켓 정보를 조회할 수 없습니다.");
            }

            // ownerId 또는 sellerId 추출
            JsonNode ownerIdNode = dataNode.get("ownerId");
            JsonNode sellerIdNode = dataNode.get("sellerId");
            
            Long sellerId = null;
            if (sellerIdNode != null && !sellerIdNode.isNull()) {
                sellerId = sellerIdNode.asLong();
            } else if (ownerIdNode != null && !ownerIdNode.isNull()) {
                sellerId = ownerIdNode.asLong();
            }

            if (sellerId == null) {
                log.error("티켓 정보에 판매자 ID가 없습니다: ticketId={}", ticketId);
                throw new IllegalStateException("티켓 정보에 판매자 ID가 없습니다.");
            }

            log.info("티켓 정보 조회 성공: ticketId={}, sellerId={}", ticketId, sellerId);
            return sellerId;
        } catch (WebClientResponseException.NotFound e) {
            log.error("티켓을 찾을 수 없습니다: ticketId={}", ticketId, e);
            throw new IllegalStateException("티켓을 찾을 수 없습니다.");
        } catch (WebClientResponseException e) {
            log.error("티켓 서비스 호출 오류: ticketId={}, status={}, body={}", 
                    ticketId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new IllegalStateException("티켓 정보 조회 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("티켓 정보 조회 중 예상치 못한 오류: ticketId={}", ticketId, e);
            throw new IllegalStateException("티켓 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
