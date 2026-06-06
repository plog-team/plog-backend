package com.plog.api.domain.aichat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findBySessionOrderByCreatedAtAsc(AiChatSession session);
    
}
