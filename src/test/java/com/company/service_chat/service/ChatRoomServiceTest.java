package com.company.service_chat.service;

import com.company.service_chat.dto.ChatMessageResponse;
import com.company.service_chat.dto.ChatRoomResponse;
import com.company.service_chat.entity.ChatMember;
import com.company.service_chat.entity.ChatMessage;
import com.company.service_chat.entity.ChatRoom;
import com.company.service_chat.entity.ChatRoom.DealStatus;
import com.company.service_chat.entity.ChatRoom.RoomStatus;
import com.company.service_chat.repository.ChatMemberRepository;
import com.company.service_chat.repository.ChatMessageRepository;
import com.company.service_chat.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomService 단위 테스트")
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private TicketLookupService ticketLookupService;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private ChatRoom testChatRoom;
    private ChatMember testChatMember;

    @BeforeEach
    void setUp() {
        // ChatRoom 생성 시 reflection으로 chatroomId 설정
        testChatRoom = ChatRoom.builder()
                .ticketId(1L)
                .buyerId(100L)
                .sellerId(200L)
                .build();
        testChatRoom.prePersist();

        testChatMember = ChatMember.builder()
                .userId(100L)
                .chatroomId(1L)
                .build();
    }

    @Test
    @DisplayName("새로운 채팅방 생성 성공")
    void createRoom_Success() throws Exception {
        // given
        Long ticketId = 1L;
        Long buyerId = 100L;
        Long sellerId = 200L;

        // chatroomId를 reflection으로 설정
        ChatRoom savedRoom = ChatRoom.builder()
                .ticketId(1L)
                .buyerId(100L)
                .sellerId(200L)
                .build();
        savedRoom.prePersist();
        java.lang.reflect.Field field = ChatRoom.class.getDeclaredField("chatroomId");
        field.setAccessible(true);
        field.set(savedRoom, 1L);

        when(chatRoomRepository.findFirstByTicketIdAndBuyerIdOrderByCreatedAtDesc(ticketId, buyerId))
                .thenReturn(Optional.empty());
        when(ticketLookupService.getSellerId(ticketId)).thenReturn(sellerId);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(savedRoom);
        when(chatMemberRepository.findByUserIdAndChatroomId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(chatService.saveSystemMessage(anyLong(), anyLong(), any(), anyString(), any()))
                .thenReturn(null);

        // when
        ChatRoomResponse response = chatRoomService.createRoom(ticketId, buyerId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTicketId()).isEqualTo(ticketId);
        assertThat(response.getRoomStatus()).isEqualTo("OPEN");
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        verify(chatMemberRepository, times(2)).save(any(ChatMember.class));
        verify(chatService, times(2)).saveSystemMessage(anyLong(), anyLong(), any(), anyString(), any());
    }

    @Test
    @DisplayName("기존 채팅방이 있고 삭제되지 않은 경우 기존 채팅방 반환")
    void createRoom_ExistingRoom_NotDeleted() throws Exception {
        // given
        Long ticketId = 1L;
        Long buyerId = 100L;

        // chatroomId 설정
        java.lang.reflect.Field field = ChatRoom.class.getDeclaredField("chatroomId");
        field.setAccessible(true);
        field.set(testChatRoom, 1L);

        when(chatRoomRepository.findFirstByTicketIdAndBuyerIdOrderByCreatedAtDesc(ticketId, buyerId))
                .thenReturn(Optional.of(testChatRoom));
        when(chatMemberRepository.findByUserIdAndChatroomId(buyerId, 1L))
                .thenReturn(Optional.of(testChatMember));

        // when
        ChatRoomResponse response = chatRoomService.createRoom(ticketId, buyerId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTicketId()).isEqualTo(ticketId);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("채팅방 목록 조회 성공")
    void getChatRoomsByUserId_Success() throws Exception {
        // given
        Long userId = 100L;

        // chatroomId 설정
        java.lang.reflect.Field field = ChatRoom.class.getDeclaredField("chatroomId");
        field.setAccessible(true);
        field.set(testChatRoom, 1L);

        List<ChatMember> members = Arrays.asList(testChatMember);
        List<ChatRoom> rooms = Arrays.asList(testChatRoom);
        ChatMessage lastMessage = ChatMessage.builder()
                .chatroomId(1L)
                .senderId(200L)
                .type(ChatMessage.MessageType.TEXT)
                .content("테스트 메시지")
                .build();

        when(chatMemberRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(members);
        when(chatRoomRepository.findAllById(anyList())).thenReturn(rooms);
        when(chatMessageRepository.findTopByChatroomIdOrderBySentAtDesc(1L)).thenReturn(lastMessage);
        when(chatMessageRepository.countByChatroomIdAndMessageIdGreaterThanAndType(eq(1L), anyLong(), any()))
                .thenReturn(3);

        // when
        List<ChatRoomResponse> responses = chatRoomService.getChatRoomsByUserId(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getLastMessageContent()).isEqualTo("테스트 메시지");
        assertThat(responses.get(0).getUnreadCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("양도 요청 처리 성공")
    void handleDealRequest_Success() throws Exception {
        // given
        Long chatroomId = 1L;
        Long buyerId = 100L;

        // chatroomId 설정
        java.lang.reflect.Field field = ChatRoom.class.getDeclaredField("chatroomId");
        field.setAccessible(true);
        field.set(testChatRoom, 1L);

        ChatMessageResponse mockResponse = ChatMessageResponse.builder()
                .messageId(1L)
                .senderId(buyerId)
                .type("SYSTEM_ACTION_MESSAGE")
                .content("시스템 메시지")
                .build();

        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(testChatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testChatRoom);
        when(chatService.saveSystemMessage(anyLong(), anyLong(), any(), anyString(), any()))
                .thenReturn(ChatMessage.builder().build());
        when(chatService.toResponse(any())).thenReturn(mockResponse);

        // when
        chatRoomService.handleDealRequest(chatroomId, buyerId);

        // then
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        verify(chatService, times(1)).saveSystemMessage(anyLong(), anyLong(), any(), anyString(), any());
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("양도 요청 - 구매자가 아닌 경우 실패")
    void handleDealRequest_NotBuyer_Fail() {
        // given
        Long chatroomId = 1L;
        Long wrongUserId = 999L;

        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(testChatRoom));

        // when & then
        assertThatThrownBy(() -> chatRoomService.handleDealRequest(chatroomId, wrongUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("양도 요청을 할 수 없습니다");
    }

    @Test
    @DisplayName("양도 수락 처리 성공")
    void handleDealAccept_Success() throws Exception {
        // given
        Long chatroomId = 1L;
        Long sellerId = 200L;

        // chatroomId 설정
        java.lang.reflect.Field field = ChatRoom.class.getDeclaredField("chatroomId");
        field.setAccessible(true);
        field.set(testChatRoom, 1L);

        ChatMessageResponse mockResponse = ChatMessageResponse.builder()
                .messageId(1L)
                .senderId(sellerId)
                .type("SYSTEM_ACTION_MESSAGE")
                .content("시스템 메시지")
                .build();

        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(testChatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testChatRoom);
        when(chatService.saveSystemMessage(anyLong(), anyLong(), any(), anyString(), any()))
                .thenReturn(ChatMessage.builder().build());
        when(chatService.toResponse(any())).thenReturn(mockResponse);

        // when
        chatRoomService.handleDealAccept(chatroomId, sellerId);

        // then
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        verify(chatService, times(1)).saveSystemMessage(anyLong(), anyLong(), any(), anyString(), any());
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("양도 수락 - 판매자가 아닌 경우 실패")
    void handleDealAccept_NotSeller_Fail() {
        // given
        Long chatroomId = 1L;
        Long wrongUserId = 999L;

        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(testChatRoom));

        // when & then
        assertThatThrownBy(() -> chatRoomService.handleDealAccept(chatroomId, wrongUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("판매자만");
    }

    @Test
    @DisplayName("양도 거절 처리 성공")
    void handleDealReject_Success() {
        // given
        Long chatroomId = 1L;
        Long sellerId = 200L;

        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(testChatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testChatRoom);
        when(chatService.saveSystemMessage(anyLong(), anyLong(), any(), anyString(), any()))
                .thenReturn(null);

        // when
        chatRoomService.handleDealReject(chatroomId, sellerId);

        // then
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        verify(chatService, times(1)).saveSystemMessage(anyLong(), anyLong(), any(), anyString(), any());
    }

    @Test
    @DisplayName("채팅방 삭제(숨김 처리) 성공")
    void deleteChatRoomForUser_Success() {
        // given
        Long chatroomId = 1L;
        Long userId = 100L;

        when(chatMemberRepository.findByUserIdAndChatroomId(userId, chatroomId))
                .thenReturn(Optional.of(testChatMember));

        // when
        chatRoomService.deleteChatRoomForUser(chatroomId, userId);

        // then
        verify(chatMemberRepository, times(1)).save(any(ChatMember.class));
        assertThat(testChatMember.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("채팅방 삭제 - 멤버가 아닌 경우 실패")
    void deleteChatRoomForUser_NotMember_Fail() {
        // given
        Long chatroomId = 1L;
        Long userId = 999L;

        when(chatMemberRepository.findByUserIdAndChatroomId(userId, chatroomId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomService.deleteChatRoomForUser(chatroomId, userId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("멤버가 아닙니다");
    }

    @Test
    @DisplayName("채팅방 상태 조회 성공")
    void getRoomStatus_Success() {
        // given
        Long chatroomId = 1L;
        when(chatRoomRepository.findById(chatroomId)).thenReturn(Optional.of(testChatRoom));

        // when
        RoomStatus status = chatRoomService.getRoomStatus(chatroomId);

        // then
        assertThat(status).isEqualTo(RoomStatus.OPEN);
    }

    @Test
    @DisplayName("메시지 읽음 처리 성공")
    void markMessagesAsRead_Success() {
        // given
        Long chatroomId = 1L;
        Long userId = 100L;
        ChatMessage latestMessage = ChatMessage.builder()
                .messageId(999L)
                .chatroomId(chatroomId)
                .senderId(200L)
                .type(ChatMessage.MessageType.TEXT)
                .content("최신 메시지")
                .build();

        when(chatMessageRepository.findTopByChatroomIdOrderBySentAtDesc(chatroomId))
                .thenReturn(latestMessage);
        when(chatMemberRepository.findByUserIdAndChatroomId(userId, chatroomId))
                .thenReturn(Optional.of(testChatMember));

        // when
        chatRoomService.markMessagesAsRead(chatroomId, userId);

        // then
        assertThat(testChatMember.getLastReadMessageId()).isEqualTo(999L);
    }

    @Test
    @DisplayName("메시지 읽음 처리 - 메시지가 없는 경우")
    void markMessagesAsRead_NoMessages() {
        // given
        Long chatroomId = 1L;
        Long userId = 100L;

        when(chatMessageRepository.findTopByChatroomIdOrderBySentAtDesc(chatroomId))
                .thenReturn(null);

        // when
        chatRoomService.markMessagesAsRead(chatroomId, userId);

        // then
        verify(chatMemberRepository, never()).findByUserIdAndChatroomId(anyLong(), anyLong());
    }
}
