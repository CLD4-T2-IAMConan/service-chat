package com.company.service_chat.repository;

import com.company.service_chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // 같은 티켓과 구매자 조합의 채팅방이 이미 존재하는지 확인 (가장 최근 것만)
    Optional<ChatRoom> findFirstByTicketIdAndBuyerIdOrderByCreatedAtDesc(Long ticketId, Long buyerId);
}