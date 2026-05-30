package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {
    Optional<MatchParticipant> findByExchangeMatchId(Long matchId);
}