package com.plog.api.domain.aiguide;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllBySessionIdOrderByOrderIdxAsc(Long sessionId);
    int countBySessionId(Long sessionId);
}
