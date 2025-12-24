package com.company.service_chat.controller;

import com.company.service_chat.dto.ApiResponse;
import com.company.service_chat.entity.ChatRoom;
import com.company.service_chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/chat/rooms")
@RequiredArgsConstructor
public class AdminRoomStatusController {

    private final ChatRoomService chatRoomService;

    // PATCH /admin/chat/rooms/status?chatroomId=10&status=LOCK
    @PatchMapping("/status")
    public ApiResponse<Void> updateRoomStatusAsAdmin(
            @RequestParam("chatroomId") Long chatroomId,
            @RequestParam("status") ChatRoom.RoomStatus status
    ) {
        chatRoomService.updateRoomStatus(chatroomId, status);
        return ApiResponse.success(null);
    }
}
