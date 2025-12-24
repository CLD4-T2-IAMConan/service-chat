package com.company.service_chat.repository;

import com.company.service_chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // 필요 시 여기에 특정 조건의 조회 메서드 추가
    // 기본적으로는 JpaRepository를 상속받으면 ChatRoom 엔티티와 PK 타입(Long)을 기반으로
    // save(), findById(), findAll() 등의 메서드를 자동으로 사용할 수 있음!
}