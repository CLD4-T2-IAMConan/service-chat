// 채팅방의 생성, 조회, 그리고 티켓 양도 상태 변경에 대한 로직
package com.company.service_chat.service;

import com.company.service_chat.dto.ChatRoomResponse;
import com.company.service_chat.dto.ChatMessageDto; // 시스템 메시지 생성을 위해 필요
import com.company.service_chat.entity.ChatRoom;
import com.company.service_chat.entity.ChatMember;
import com.company.service_chat.entity.ChatRoom.RoomStatus;
import com.company.service_chat.entity.ChatRoom.DealStatus;
import com.company.service_chat.repository.ChatRoomRepository;
import com.company.service_chat.repository.ChatMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // 조회 메서드는 readOnly=true로 성능 최적화
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatService chatService; // 시스템 메시지 저장을 위해 주입
    // private final TicketService ticketService; // 티켓 정보 조회를 위한 서비스
    private final TicketLookupService ticketLookupService;

    // 생성자 주입
    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           ChatMemberRepository chatMemberRepository,
                           ChatService chatService,
                           TicketLookupService ticketLookupService) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.chatService = chatService;
        this.ticketLookupService = ticketLookupService;
    }

    // --- 1. 채팅방 생성 (POST /chat/rooms) ---
    @Transactional // 생성 작업은 쓰기 트랜잭션 필요
    public ChatRoomResponse createRoom(Long ticketId, Long buyerId) {

        // 1. 티켓 서비스에서 판매자 ID 조회
        // [TODO: 실제 티켓 서비스에서 sellerId를 조회하는 로직 구현]
        Long sellerId = 4L;
        // Long sellerId = ticketLookupService.getSellerId(ticketId);

        // 2. ChatRoom 생성 및 저장
        ChatRoom chatRoom = ChatRoom.builder()
                .ticketId(ticketId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .build();
        chatRoom = chatRoomRepository.save(chatRoom);
        Long chatroomId = chatRoom.getChatroomId();

        // 3. 구매자, 판매자 정보 저장
        chatMemberRepository.save(new ChatMember(buyerId, chatroomId));
        chatMemberRepository.save(new ChatMember(sellerId, chatroomId));

        // "양도 요청하기" 버튼 보여주는 metadata -----------------
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("actionType", "REQUEST_TRANSFER_INTRO");
        metadata.put("buyerId", buyerId);
        metadata.put("visibleTarget", "BUYER");

        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(Map.of(
                "label", "양도 요청하기",
                "actionCode", "TRANSFER_REQUEST",
                "isPrimary", true
        ));

        metadata.put("actions", actions);
        // 여기까지 양도 요청하기 버튼 보여주는 metadata -----------

        // 4. 시스템 메시지 저장
        chatService.saveSystemMessage(
                chatroomId,
                buyerId,
                ChatMessageDto.MessageType.SYSTEM_ACTION_MESSAGE,
                "티켓 거래를 시작하려면 '양도 요청하기' 버튼을 눌러주세요.",
                metadata
        );

        // 5. 응답 DTO 반환
        return ChatRoomResponse.builder()
                .chatroomId(chatroomId)
                .ticketId(ticketId)
                .createdAt(chatRoom.getCreatedAt())
                .roomStatus(chatRoom.getRoomStatus().name())
                .build();
    }

    // --- 2. 채팅방 목록 조회 (GET /chat/rooms) ---
    public List<ChatRoomResponse> getChatRoomsByUserId(Long userId) {
        // 1. 해당 사용자가 속한 ChatMember 목록 조회
        List<ChatMember> memberList = chatMemberRepository.findByUserIdAndIsDeletedFalse(userId);

        // 2. ChatMember 목록에서 ChatRoomId 리스트 추출
        List<Long> chatRoomIds = memberList.stream()
                .map(ChatMember::getChatroomId)
                .collect(Collectors.toList());

        // 3. ChatRoom 정보 조회 (findByIdIn 사용 가능)
        List<ChatRoom> chatRooms = chatRoomRepository.findAllById(chatRoomIds);

        // 4. Entity -> DTO 변환 및 반환
        return chatRooms.stream()
                .map(room -> ChatRoomResponse.builder()
                        .chatroomId(room.getChatroomId())
                        .ticketId(room.getTicketId())
                        .createdAt(room.getCreatedAt())
                        .roomStatus(room.getRoomStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    // --- 3. 양도 요청 처리 (구매자 -> 판매자) ---
    @Transactional
    public void handleDealRequest(Long chatroomId, Long buyerId) {
        // 1. 채팅방 조회 및 유효성 검사
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new NoSuchElementException("채팅방을 찾을 수 없습니다."));

        // 양도 요청 이전의 거래 상태가 PENDING인지 확인
        if (chatRoom.getDealStatus() != DealStatus.PENDING) {
            throw new IllegalStateException("이미 양도 절차가 진행 중입니다.");
        }

        // buyerId가 해당 채팅방 구매자인지 확인
        if (!chatRoom.getBuyerId().equals(buyerId)) {
            throw new IllegalStateException("해당 사용자는 양도 요청을 할 수 없습니다.");
        }

        // 2. DealStatus 업데이트: REQUESTED
        chatRoom.updateStatus(RoomStatus.OPEN, DealStatus.REQUESTED);
        chatRoomRepository.save(chatRoom);

        // 양도 요청 metadata 만들기 ----------------------------
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("actionType", "TICKET_REQUEST");
        metadata.put("sellerId", chatRoom.getSellerId());
        metadata.put("visibleTarget", "SELLER");

        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(Map.of(
                "label", "양도 수락",
                "actionCode", "TRANSFER_ACCEPT",
                "isPrimary", true
        ));
        actions.add(Map.of(
                "label", "양도 거절",
                "actionCode", "TRANSFER_REJECT",
                "isPrimary", false
        ));

        metadata.put("actions", actions);
        // 여기까지 양도 요청 metadata 만들기 --------------------

        // 3. 시스템 메시지 저장
        // 이 메시지는 ChatController에서 웹소켓으로 발송되는 것
        chatService.saveSystemMessage(
                chatroomId,
                buyerId,
                ChatMessageDto.MessageType.SYSTEM_ACTION_MESSAGE,
                "구매자가 티켓 양도를 신청했습니다. 수락/거절을 선택해주세요.",
                metadata
        );
    }

    // --- 4. 양도 수락 (결제 요청) 처리 (판매자) ---
    @Transactional
    public void handleDealAccept(Long chatroomId, Long sellerId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new NoSuchElementException("채팅방을 찾을 수 없습니다."));

        if (!chatRoom.getSellerId().equals(sellerId)) {
            throw new IllegalStateException("판매자만 양도 수락을 할 수 있습니다.");
        }

        // 1. DealStatus 업데이트: ACCEPTED
        chatRoom.updateStatus(RoomStatus.OPEN, DealStatus.ACCEPTED); // 방 상태는 OPEN 유지
        chatRoomRepository.save(chatRoom);

        // 양도 수락 (결제 요청) metadata 만들기 -----------------
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("actionType", "PAYMENT_REQUEST");
        metadata.put("buyerId", chatRoom.getBuyerId());
        metadata.put("visibleTarget", "BUYER");

        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(Map.of(
                "label", "결제하기",
                "actionCode", "START_PAYMENT",
                "isPrimary", true
        ));

        metadata.put("actions", actions);
        // 여기까지 양도 수락 (결제 요청) metadata ----------------

        // 2. 결제 요청 시스템 메시지 저장
        chatService.saveSystemMessage(
                chatroomId,
                sellerId,
                ChatMessageDto.MessageType.SYSTEM_ACTION_MESSAGE,
                "양도가 수락되었습니다. 구매자는 24시간 이내에 결제를 진행해주세요.",
                metadata
        );
    }

    // --- 5. 양도 거절 처리 (판매자) ---
    @Transactional
    public void handleDealReject(Long chatroomId, Long sellerId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new NoSuchElementException("채팅방을 찾을 수 없습니다."));

        // 1. RoomStatus를 LOCK으로 변경, DealStatus를 REJECTED로 변경
        chatRoom.updateStatus(RoomStatus.LOCK, DealStatus.REJECTED);
        chatRoomRepository.save(chatRoom);

        // 양도 거절 metadata 만들기 ----------------------------
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("actionType", "TICKET_REJECT");
        metadata.put("reason", "판매자 거절");
        metadata.put("visibleTarget", "BUYER");
        // 여기까지 양도 거절 metadata --------------------------

        // 2. 거절 시스템 메시지 저장
        chatService.saveSystemMessage(
                chatroomId,
                sellerId,
                ChatMessageDto.MessageType.SYSTEM_INFO_MESSAGE,
                "판매자가 양도를 거절했습니다. 채팅이 잠금 상태로 전환됩니다.",
                metadata
        );
    }

    // 6. 채팅방 삭제(숨김 처리)
    @Transactional
    public void deleteChatRoomForUser(Long chatroomId, Long userId) {

        ChatMember chatMember = chatMemberRepository
                .findByUserIdAndChatroomId(userId, chatroomId)
                .orElseThrow(() -> new NoSuchElementException("해당 채팅방의 멤버가 아닙니다."));

        chatMember.markAsDeleted();
    }

    // 1. 현재 채팅방 상태 조회
    public ChatRoom.RoomStatus getRoomStatus(Long chatroomId) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new NoSuchElementException("채팅방을 찾을 수 없습니다."));

        return chatRoom.getRoomStatus(); // OPEN / LOCK / DONE
    }

    // 2. 채팅방 상태 변경
    @Transactional
    public void updateRoomStatus(Long chatroomId, ChatRoom.RoomStatus newRoomStatus) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new NoSuchElementException("채팅방을 찾을 수 없습니다."));

        // 기존 dealStatus 유지
        ChatRoom.DealStatus currentDealStatus = chatRoom.getDealStatus();

        // 엔티티의 updateStatus 호출
        chatRoom.updateStatus(newRoomStatus, currentDealStatus);
    }
}