package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.ExchangeSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeSessionRepository extends JpaRepository<ExchangeSession, Long> {
}