package com.company.service_chat.controller;

import com.company.service_chat.dto.ApiResponse;
import com.company.service_chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat/deal")
@RequiredArgsConstructor
public class DealController {
    private final ChatRoomService chatRoomService;

    // 1. 구매자 → 판매자 : 양도 요청
    // POST /chat/deal/request
    @PostMapping("/request")
    public ApiResponse<Void> requestDeal(
            @RequestParam Long chatroomId,
            @RequestParam Long buyerId
    ) {
        chatRoomService.handleDealRequest(chatroomId, buyerId);
        return ApiResponse.success(null);
    }

    // 2. 판매자 → 양도 수락
    // POST /chat/deal/accept
    @PostMapping("/accept")
    public ApiResponse<Void> acceptDeal(
            @RequestParam Long chatroomId,
            @RequestParam Long sellerId
    ) {
        chatRoomService.handleDealAccept(chatroomId, sellerId);
        return ApiResponse.success(null);
    }

    // 3. 판매자 → 양도 거절
    // POST /chat/deal/reject
    @PostMapping("/reject")
    public ApiResponse<Void> rejectDeal(
            @RequestParam Long chatroomId,
            @RequestParam Long sellerId
    ) {
        chatRoomService.handleDealReject(chatroomId, sellerId);
        return ApiResponse.success(null);
    }
}
