package com.plog.api.domain.aiguide;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSessionRepository extends JpaRepository<AiSession, Long> {
    List<AiSession> findAllByUserIdOrderByIdDesc(Long userId);
    List<AiSession> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
