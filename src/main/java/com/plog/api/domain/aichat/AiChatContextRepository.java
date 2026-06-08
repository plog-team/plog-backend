package com.plog.api.domain.aichat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.plog.api.domain.user.User;

public interface AiChatContextRepository extends JpaRepository<AiChatSession, Long> {
    Optional<AiChatContext> findByUser(User user);
    
}
