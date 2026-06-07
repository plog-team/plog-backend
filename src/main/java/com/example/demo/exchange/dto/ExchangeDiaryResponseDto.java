package com.example.demo.exchange.dto;

import com.example.demo.exchange.domain.ExchangeDiary;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ExchangeDiaryResponseDto {

    private Long id;
    private Long sessionId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private int dayNumber;

    public ExchangeDiaryResponseDto(ExchangeDiary diary) {
        this.id = diary.getId();
        this.sessionId = diary.getExchangeSession().getId();
        this.userId = diary.getUserId();
        this.content = diary.getContent();
        this.createdAt = diary.getCreatedAt();
        this.dayNumber = diary.getDayNumber();
    }
}