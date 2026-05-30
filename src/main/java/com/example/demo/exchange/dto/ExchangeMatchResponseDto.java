package com.example.demo.exchange.dto;

import com.example.demo.exchange.domain.ExchangeMatch;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ExchangeMatchResponseDto {

    private Long id;
    private String status;
    private LocalDateTime createdAt;
    private String requesterNickname;

    public ExchangeMatchResponseDto(ExchangeMatch match) {
        this.id = match.getId();
        this.status = match.getStatus();
        this.createdAt = match.getCreatedAt();
    }

    public ExchangeMatchResponseDto(ExchangeMatch match, String requesterNickname) {
        this.id = match.getId();
        this.status = match.getStatus();
        this.createdAt = match.getCreatedAt();
        this.requesterNickname = requesterNickname;
    }
}