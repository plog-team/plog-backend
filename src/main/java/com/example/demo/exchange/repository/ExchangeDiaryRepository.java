package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.ExchangeDiary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeDiaryRepository extends JpaRepository<ExchangeDiary, Long> {
}