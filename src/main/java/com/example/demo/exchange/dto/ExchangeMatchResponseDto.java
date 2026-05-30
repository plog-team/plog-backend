package com.example.demo.exchange.dto;

import com.example.demo.exchange.domain.ExchangeMatch;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ExchangeMatchResponseDto {

    private Long id;
    private String status;
    private LocalDateTime createdAt;

    public ExchangeMatchResponseDto(ExchangeMatch match) {
        this.id = match.getId();
        this.status = match.getStatus();
        this.createdAt = match.getCreatedAt();
    }
}