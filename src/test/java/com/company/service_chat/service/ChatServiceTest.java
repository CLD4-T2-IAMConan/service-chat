package com.company.service_chat.service;

import com.company.service_chat.dto.ChatMessageDto;
import com.company.service_chat.dto.ChatMessageResponse;
import com.company.service_chat.entity.ChatMember;
import com.company.service_chat.entity.ChatMessage;
import com.company.service_chat.entity.ChatRoom;
import com.company.service_chat.repository.ChatMemberRepository;
import com.company.service_chat.repository.ChatMessageRepository;
import com.company.service_chat.repository.ChatRoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 단위 테스트")
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ChatService chatService;

    private ChatRoom testChatRoom;
    private ChatMessage testChatMessage;
    private ChatMember testChatMember;

    @BeforeEach
    void setUp() {
        testChatRoom = ChatRoom.builder()
                .ticketId(1L)
                .buyerId(100L)
                .sellerId(200L)
                .build();
        testChatRoom.prePersist();

        testChatMessage = ChatMessage.builder()
                .messageId(1L)
                .chatroomId(1L)
                .senderId(100L)
                .type(ChatMessage.MessageType.TEXT)
                .content("테스트 메시지")
                .build();

        testChatMember = ChatMember.builder()
                .userId(100L)
                .chatroomId(1L)
                .build();
    }

    @Test
    @DisplayName("일반 메시지 저장 성공 - 구매자")
    void saveMessage_Buyer_Success() {
        // given
        ChatMessageDto messageDto = ChatMessageDto.builder()
                .chatroomId(1L)
                .senderId(100L)
                .type(ChatMessageDto.MessageType.TEXT)
                .content("테스트 메시지")
                .build();

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testChatMessage);

        // when
        ChatMessage savedMessage = chatService.saveMessage(messageDto);

        // then
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getContent()).isEqualTo("테스트 메시지");
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("일반 메시지 저장 성공 - 판매자")
    void saveMessage_Seller_Success() {
        // given
        ChatMessageDto messageDto = ChatMessageDto.builder()
                .chatroomId(1L)
                .senderId(200L)
                .type(ChatMessageDto.MessageType.TEXT)
                .content("판매자 메시지")
                .build();

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testChatMessage);

        // when
        ChatMessage savedMessage = chatService.saveMessage(messageDto);

        // then
        assertThat(savedMessage).isNotNull();
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("메시지 저장 실패 - 존재하지 않는 채팅방")
    void saveMessage_ChatRoomNotFound_Fail() {
        // given
        ChatMessageDto messageDto = ChatMessageDto.builder()
                .chatroomId(999L)
                .senderId(100L)
                .type(ChatMessageDto.MessageType.TEXT)
                .content("테스트 메시지")
                .build();

        when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.saveMessage(messageDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("채팅방 없음");
    }

    @Test
    @DisplayName("메시지 저장 실패 - 잠긴 채팅방")
    void saveMessage_LockedRoom_Fail() {
        // given
        testChatRoom.updateStatus(ChatRoom.RoomStatus.LOCK, ChatRoom.DealStatus.REJECTED);

        ChatMessageDto messageDto = ChatMessageDto.builder()
                .chatroomId(1L)
                .senderId(100L)
                .type(ChatMessageDto.MessageType.TEXT)
                .content("테스트 메시지")
                .build();

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));

        // when & then
        assertThatThrownBy(() -> chatService.saveMessage(messageDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잠긴 채팅방");
    }

    @Test
    @DisplayName("메시지 저장 실패 - 참여자가 아닌 사용자")
    void saveMessage_NotParticipant_Fail() {
        // given
        ChatMessageDto messageDto = ChatMessageDto.builder()
                .chatroomId(1L)
                .senderId(999L)
                .type(ChatMessageDto.MessageType.TEXT)
                .content("테스트 메시지")
                .build();

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));

        // when & then
        assertThatThrownBy(() -> chatService.saveMessage(messageDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("참여자만");
    }

    @Test
    @DisplayName("메시지 저장 성공 - metadata 포함")
    void saveMessage_WithMetadata_Success() throws Exception {
        // given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("actionType", "TEST_ACTION");

        ChatMessageDto messageDto = ChatMessageDto.builder()
                .chatroomId(1L)
                .senderId(100L)
                .type(ChatMessageDto.MessageType.SYSTEM_ACTION_MESSAGE)
                .content("시스템 메시지")
                .metadata(metadata)
                .build();

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"actionType\":\"TEST_ACTION\"}");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testChatMessage);

        // when
        ChatMessage savedMessage = chatService.saveMessage(messageDto);

        // then
        assertThat(savedMessage).isNotNull();
        verify(objectMapper, times(1)).writeValueAsString(any());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("시스템 메시지 저장 성공")
    void saveSystemMessage_Success() {
        // given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("actionType", "TRANSFER_REQUEST");

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testChatMessage);

        // when
        ChatMessage savedMessage = chatService.saveSystemMessage(
                1L, 100L, ChatMessageDto.MessageType.SYSTEM_ACTION_MESSAGE,
                "양도 요청", metadata
        );

        // then
        assertThat(savedMessage).isNotNull();
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("채팅방 메시지 목록 조회 성공")
    void getMessagesByChatroomId_Success() {
        // given
        List<ChatMessage> messages = Arrays.asList(
                testChatMessage,
                ChatMessage.builder()
                        .messageId(2L)
                        .chatroomId(1L)
                        .senderId(200L)
                        .type(ChatMessage.MessageType.TEXT)
                        .content("두 번째 메시지")
                        .build()
        );

        when(chatMessageRepository.findByChatroomIdOrderBySentAtAsc(1L)).thenReturn(messages);

        // when
        List<ChatMessageResponse> responses = chatService.getMessagesByChatroomId(1L);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getContent()).isEqualTo("테스트 메시지");
    }

    @Test
    @DisplayName("메시지 목록 조회 - 빈 목록")
    void getMessagesByChatroomId_EmptyList() {
        // given
        when(chatMessageRepository.findByChatroomIdOrderBySentAtAsc(1L))
                .thenReturn(Collections.emptyList());

        // when
        List<ChatMessageResponse> responses = chatService.getMessagesByChatroomId(1L);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("메시지 읽음 처리 성공")
    void markAsRead_Success() {
        // given
        Long chatroomId = 1L;
        Long userId = 100L;
        Long lastReadMessageId = 50L;

        when(chatMemberRepository.findByUserIdAndChatroomId(userId, chatroomId))
                .thenReturn(Optional.of(testChatMember));

        // when
        chatService.markAsRead(chatroomId, userId, lastReadMessageId);

        // then
        assertThat(testChatMember.getLastReadMessageId()).isEqualTo(lastReadMessageId);
    }

    @Test
    @DisplayName("메시지 읽음 처리 실패 - 멤버가 아닌 경우")
    void markAsRead_NotMember_Fail() {
        // given
        Long chatroomId = 1L;
        Long userId = 999L;
        Long lastReadMessageId = 50L;

        when(chatMemberRepository.findByUserIdAndChatroomId(userId, chatroomId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.markAsRead(chatroomId, userId, lastReadMessageId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("멤버가 아닙니다");
    }

    @Test
    @DisplayName("메시지 읽음 처리 - 이전 값보다 작은 경우 업데이트 안 됨")
    void markAsRead_SmallerValue_NotUpdated() {
        // given
        Long chatroomId = 1L;
        Long userId = 100L;
        testChatMember.updateLastReadMessageId(100L);
        Long lastReadMessageId = 50L;

        when(chatMemberRepository.findByUserIdAndChatroomId(userId, chatroomId))
                .thenReturn(Optional.of(testChatMember));

        // when
        chatService.markAsRead(chatroomId, userId, lastReadMessageId);

        // then
        assertThat(testChatMember.getLastReadMessageId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("ChatMessage를 ChatMessageResponse로 변환 성공")
    void toResponse_Success() throws Exception {
        // given
        String metadataJson = "{\"actionType\":\"TEST\"}";
        ChatMessage message = ChatMessage.builder()
                .messageId(1L)
                .chatroomId(1L)
                .senderId(100L)
                .type(ChatMessage.MessageType.TEXT)
                .content("테스트")
                .metadata(metadataJson)
                .build();

        Map<String, Object> metadataObj = new HashMap<>();
        metadataObj.put("actionType", "TEST");

        when(objectMapper.readValue(metadataJson, Object.class)).thenReturn(metadataObj);

        // when
        ChatMessageResponse response = chatService.toResponse(message);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessageId()).isEqualTo(1L);
        assertThat(response.getContent()).isEqualTo("테스트");
        assertThat(response.getMetadata()).isNotNull();
    }

    @Test
    @DisplayName("ChatMessage를 ChatMessageResponse로 변환 - metadata null")
    void toResponse_NullMetadata_Success() {
        // given
        ChatMessage message = ChatMessage.builder()
                .messageId(1L)
                .chatroomId(1L)
                .senderId(100L)
                .type(ChatMessage.MessageType.TEXT)
                .content("테스트")
                .metadata(null)
                .build();

        // when
        ChatMessageResponse response = chatService.toResponse(message);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMetadata()).isNull();
    }
}
