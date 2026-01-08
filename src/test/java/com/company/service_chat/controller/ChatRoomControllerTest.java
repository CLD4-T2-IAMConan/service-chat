package com.company.service_chat.controller;

import com.company.service_chat.dto.*;
import com.company.service_chat.entity.ChatMessage;
import com.company.service_chat.service.ChatRoomService;
import com.company.service_chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRoomController.class)
@ActiveProfiles("test")
@DisplayName("ChatRoomController 통합 테스트")
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatRoomService chatRoomService;

    @MockBean
    private ChatService chatService;

    @Test
    @DisplayName("채팅방 생성 API 성공")
    void createChatRoom_Success() throws Exception {
        // given
        ChatRoomResponse response = ChatRoomResponse.builder()
                .chatroomId(1L)
                .ticketId(1L)
                .createdAt(LocalDateTime.now())
                .roomStatus("OPEN")
                .build();

        when(chatRoomService.createRoom(anyLong(), anyLong())).thenReturn(response);

        String requestJson = "{\"ticketId\":1,\"buyerId\":100}";

        // when & then
        mockMvc.perform(post("/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chatroomId").value(1))
                .andExpect(jsonPath("$.data.roomStatus").value("OPEN"));

        verify(chatRoomService, times(1)).createRoom(anyLong(), anyLong());
    }

    @Test
    @DisplayName("채팅방 목록 조회 API 성공")
    void getChatRooms_Success() throws Exception {
        // given
        Long userId = 100L;
        ChatRoomResponse room1 = ChatRoomResponse.builder()
                .chatroomId(1L)
                .ticketId(1L)
                .createdAt(LocalDateTime.now())
                .roomStatus("OPEN")
                .lastMessageContent("안녕하세요")
                .lastMessageType("TEXT")
                .unreadCount(3)
                .build();

        ChatRoomResponse room2 = ChatRoomResponse.builder()
                .chatroomId(2L)
                .ticketId(2L)
                .createdAt(LocalDateTime.now())
                .roomStatus("OPEN")
                .lastMessageContent("감사합니다")
                .lastMessageType("TEXT")
                .unreadCount(1)
                .build();

        List<ChatRoomResponse> rooms = Arrays.asList(room1, room2);

        when(chatRoomService.getChatRoomsByUserId(userId)).thenReturn(rooms);

        // when & then
        mockMvc.perform(get("/chat/rooms")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].chatroomId").value(1))
                .andExpect(jsonPath("$.data[0].unreadCount").value(3))
                .andExpect(jsonPath("$.data[1].chatroomId").value(2))
                .andExpect(jsonPath("$.data[1].unreadCount").value(1));

        verify(chatRoomService, times(1)).getChatRoomsByUserId(userId);
    }

    @Test
    @DisplayName("메시지 목록 조회 API 성공")
    void getChatMessages_Success() throws Exception {
        // given
        Long chatroomId = 1L;
        ChatMessageResponse message1 = ChatMessageResponse.builder()
                .messageId(1L)
                .senderId(100L)
                .type("TEXT")
                .content("첫 번째 메시지")
                .sentAt(LocalDateTime.now())
                .build();

        ChatMessageResponse message2 = ChatMessageResponse.builder()
                .messageId(2L)
                .senderId(200L)
                .type("TEXT")
                .content("두 번째 메시지")
                .sentAt(LocalDateTime.now())
                .build();

        List<ChatMessageResponse> messages = Arrays.asList(message1, message2);

        when(chatService.getMessagesByChatroomId(chatroomId)).thenReturn(messages);

        // when & then
        mockMvc.perform(get("/chat/rooms/{chatroomId}/messages", chatroomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].content").value("첫 번째 메시지"))
                .andExpect(jsonPath("$.data[1].content").value("두 번째 메시지"));

        verify(chatService, times(1)).getMessagesByChatroomId(chatroomId);
    }

    @Test
    @DisplayName("메시지 읽음 처리 API 성공")
    void markAsRead_Success() throws Exception {
        // given
        Long chatroomId = 1L;
        Long userId = 100L;
        Long lastReadMessageId = 50L;

        doNothing().when(chatService).markAsRead(chatroomId, userId, lastReadMessageId);

        // when & then
        mockMvc.perform(post("/chat/rooms/{chatroomId}/read", chatroomId)
                        .param("userId", userId.toString())
                        .param("lastReadMessageId", lastReadMessageId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(chatService, times(1)).markAsRead(chatroomId, userId, lastReadMessageId);
    }

    @Test
    @DisplayName("채팅방 입장 시 모든 메시지 읽음 처리 API 성공")
    void markAllAsRead_Success() throws Exception {
        // given
        Long chatroomId = 1L;
        Long userId = 100L;

        doNothing().when(chatRoomService).markMessagesAsRead(chatroomId, userId);

        // when & then
        mockMvc.perform(put("/chat/rooms/{chatroomId}/read-all", chatroomId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(chatRoomService, times(1)).markMessagesAsRead(chatroomId, userId);
    }

    @Test
    @DisplayName("채팅방 삭제 API 성공")
    void deleteChatRoom_Success() throws Exception {
        // given
        Long chatroomId = 1L;
        Long userId = 100L;

        doNothing().when(chatRoomService).deleteChatRoomForUser(chatroomId, userId);

        // when & then
        mockMvc.perform(delete("/chat/rooms/{chatroomId}", chatroomId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(chatRoomService, times(1)).deleteChatRoomForUser(chatroomId, userId);
    }

    @Test
    @DisplayName("시스템 액션 - 양도 요청 API 성공")
    void handleSystemAction_TransferRequest_Success() throws Exception {
        // given
        SystemActionRequest request = new SystemActionRequest();
        request.setChatroomId(1L);
        request.setActionCode("TRANSFER_REQUEST");
        Long userId = 100L;

        doNothing().when(chatRoomService).handleDealRequest(anyLong(), eq(userId));

        // when & then
        mockMvc.perform(post("/chat/rooms/system-action")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("시스템 액션 - 양도 수락 API 성공")
    void handleSystemAction_TransferAccept_Success() throws Exception {
        // given
        SystemActionRequest request = new SystemActionRequest();
        request.setChatroomId(1L);
        request.setActionCode("TRANSFER_ACCEPT");
        Long userId = 200L;

        doNothing().when(chatRoomService).handleDealAccept(anyLong(), eq(userId));

        // when & then
        mockMvc.perform(post("/chat/rooms/system-action")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("시스템 액션 - 양도 거절 API 성공")
    void handleSystemAction_TransferReject_Success() throws Exception {
        // given
        SystemActionRequest request = new SystemActionRequest();
        request.setChatroomId(1L);
        request.setActionCode("TRANSFER_REJECT");
        Long userId = 200L;

        doNothing().when(chatRoomService).handleDealReject(anyLong(), eq(userId));

        // when & then
        mockMvc.perform(post("/chat/rooms/system-action")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("시스템 액션 - 결제 시작 API 성공")
    void handleSystemAction_StartPayment_Success() throws Exception {
        // given
        SystemActionRequest request = new SystemActionRequest();
        request.setChatroomId(1L);
        request.setActionCode("START_PAYMENT");
        Long userId = 100L;

        // when & then
        mockMvc.perform(post("/chat/rooms/system-action")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
