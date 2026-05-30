package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.ExchangeRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRoomRepository extends JpaRepository<ExchangeRoom, Long> {
}