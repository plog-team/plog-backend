package com.plog.api.exchange.repository;

import com.plog.api.exchange.domain.ExchangeDiary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeDiaryRepository extends JpaRepository<ExchangeDiary, Long> {
}