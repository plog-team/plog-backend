package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Long> {
    List<SessionParticipant> findByExchangeSessionId(Long sessionId);
}