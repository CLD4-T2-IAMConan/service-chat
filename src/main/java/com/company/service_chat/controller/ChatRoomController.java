package com.company.service_chat.controller;

import com.company.service_chat.dto.*;
import com.company.service_chat.service.ChatRoomService;
import com.company.service_chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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

    // 4-1. 채팅방 입장 시 모든 메시지 읽음 처리 (PUT /chat/rooms/{chatroomId}/read-all?userId=1)
    @PutMapping("/{chatroomId}/read-all")
    public ApiResponse<Void> markAllAsRead(
            @PathVariable Long chatroomId,
            @RequestParam Long userId) {

        chatRoomService.markMessagesAsRead(chatroomId, userId);
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
    public ApiResponse<Void> handleSystemAction(
            @RequestBody SystemActionRequest request,
            @RequestParam Long userId
    ) {
        try {
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

                case "START_PAYMENT":
                    // 결제 시작은 프론트엔드에서 처리하므로 여기서는 성공 응답만 반환
                    // 실제 결제 처리는 결제 서비스에서 처리됨
                    break;

                default:
                    throw new IllegalArgumentException("알 수 없는 actionCode: " + request.getActionCode());
            }
            return ApiResponse.success(null);
        } catch (Exception e) {
            // 예외는 GlobalExceptionHandler에서 처리되지만, 여기서도 로깅
            throw e;
        }
    }

}
