package com.company.service_chat.repository;

import com.company.service_chat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    // 특정 사용자가 참여한 채팅방 목록을 조회할 때 사용, 채팅방 목록 화면이 있으니까 필요한 메서드
    List<ChatMember> findByUserIdAndIsDeletedFalse(Long userId);

    // 특정 방의 특정 사용자 정보를 찾을 때 사용
    Optional<ChatMember> findByChatroomIdAndUserId(Long chatroomId, Long userId);
}