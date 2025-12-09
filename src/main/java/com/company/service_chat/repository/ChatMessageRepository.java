package com.company.service_chat.repository;

import com.company.service_chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 채팅방의 메시지 목록을 오래된 순서대로 조회할 때 사용. (선택)
    // 즉 한 사람에게 메시지가 여러 개 오면 채팅방 들어갔을 때 안 읽은 메시지 중 제일 오래된 것부터 보여주는 기능을 넣을 때 사용
    // "메시지 목록 조회" 기능에 사용
    List<ChatMessage> findByChatroomIdOrderBySentAtAsc(Long chatroomId);
    // lastReadMessageId 이후의 메시지만 가져올 때 사용
    List<ChatMessage> findByChatroomIdAndMessageIdGreaterThan(Long chatroomId, Long messageId);
}