package com.company.service_chat.controller;

import com.company.service_chat.dto.ApiResponse;
import com.company.service_chat.entity.ChatRoom;
import com.company.service_chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat/rooms/status")
@RequiredArgsConstructor
public class RoomStatusController {

    private final ChatRoomService chatRoomService;

    // 1. 채팅방 상태 조회
    // GET /chat/rooms/status?chatroomId=10
    @GetMapping
    public ApiResponse<ChatRoom.RoomStatus> getRoomStatus(
            @RequestParam Long chatroomId
    ) {
        ChatRoom.RoomStatus status = chatRoomService.getRoomStatus(chatroomId);
        return ApiResponse.success(status);
    }

    // 2. 채팅방 상태 변경 (OPEN / LOCK)
    // PATCH /chat/rooms/status?chatroomId=10&status=LOCK
    @PatchMapping
    public ApiResponse<Void> updateRoomStatus(
            @RequestParam Long chatroomId,
            @RequestParam ChatRoom.RoomStatus status
    ) {
        chatRoomService.updateRoomStatus(chatroomId, status);
        return ApiResponse.success(null);
    }
}
