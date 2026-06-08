package com.plog.api.exchange.repository;

import com.plog.api.exchange.domain.ExchangeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExchangeSessionRepository extends JpaRepository<ExchangeSession, Long> {
    Optional<ExchangeSession> findByExchangeRoomId(Long roomId);
}