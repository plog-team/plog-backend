package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Long> {
}