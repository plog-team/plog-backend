package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {
}