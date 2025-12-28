package com.company.service_chat.controller;

import com.company.service_chat.dto.*;
import com.company.service_chat.service.ChatRoomService;
import com.company.service_chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;

    // 1. 채팅방 생성 (POST /chat/rooms)
    @PostMapping
    public ApiResponse<ChatRoomResponse> createChatRoom(@RequestBody ChatRoomRequest request) {
        ChatRoomResponse response = chatRoomService.createRoom(
                request.getTicketId(),
                request.getBuyerId()
        );
        return ApiResponse.success(response);
    }

    // 2. 채팅방 목록 조회 (GET /chat/rooms?userId=1)
    @GetMapping
    public ApiResponse<List<ChatRoomResponse>> getChatRooms(@RequestParam Long userId) {
        List<ChatRoomResponse> rooms = chatRoomService.getChatRoomsByUserId(userId);
        return ApiResponse.success(rooms);
    }

    // 3. 메시지 목록 조회 (GET /chat/rooms/{chatroomId}/messages)
    @GetMapping("/{chatroomId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getChatMessages(@PathVariable Long chatroomId) {
        List<ChatMessageResponse> messages = chatService.getMessagesByChatroomId(chatroomId);
        return ApiResponse.success(messages);
    }

    // 4. 메시지 읽음 처리 (POST /chat/rooms/{chatroomId}/read?userId=1&lastReadMessageId=33)
    @PostMapping("/{chatroomId}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable Long chatroomId,
            @RequestParam Long userId,
            @RequestParam Long lastReadMessageId) {

        chatService.markAsRead(chatroomId, userId, lastReadMessageId);
        return ApiResponse.success(null);
    }

    // 5. 사용자 기준으로 채팅방 삭제 (DELETE /chat/rooms/{chatroomId}?userId=1)
    @DeleteMapping("/{chatroomId}")
    public ApiResponse<Void> deleteChatRoom(
            @PathVariable Long chatroomId,
            @RequestParam Long userId) {

        chatRoomService.deleteChatRoomForUser(chatroomId, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/system-action")
    public void handleSystemAction(
            @RequestBody SystemActionRequest request,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        switch (request.getActionCode()) {
            case "TRANSFER_REQUEST":
                chatRoomService.handleDealRequest(
                        request.getChatroomId(),
                        userId
                );
                break;

            case "TRANSFER_ACCEPT":
                chatRoomService.handleDealAccept(
                        request.getChatroomId(),
                        userId
                );
                break;

            case "TRANSFER_REJECT":
                chatRoomService.handleDealReject(
                        request.getChatroomId(),
                        userId
                );
                break;

            default:
                throw new IllegalArgumentException("알 수 없는 actionCode");
        }
    }

}
