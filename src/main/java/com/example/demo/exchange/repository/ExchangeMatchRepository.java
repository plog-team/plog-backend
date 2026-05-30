package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.ExchangeMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeMatchRepository extends JpaRepository<ExchangeMatch, Long> {
}