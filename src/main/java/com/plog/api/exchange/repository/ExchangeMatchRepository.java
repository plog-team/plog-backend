package com.plog.api.exchange.repository;

import com.plog.api.exchange.domain.ExchangeMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExchangeMatchRepository extends JpaRepository<ExchangeMatch, Long> {
    List<ExchangeMatch> findByStatus(String status);
}