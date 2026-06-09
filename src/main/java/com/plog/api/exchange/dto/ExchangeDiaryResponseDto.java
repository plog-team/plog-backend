package com.plog.api.exchange.dto;

import com.plog.api.exchange.domain.ExchangeDiary;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ExchangeDiaryResponseDto {

    private Long id;
    private Long sessionId;
    private Long userId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private int dayNumber;

    public ExchangeDiaryResponseDto(ExchangeDiary diary) {
        this.id = diary.getId();
        this.sessionId = diary.getExchangeSession().getId();
        this.userId = diary.getUserId();
        this.title = diary.getTitle();
        this.content = diary.getContent();
        this.createdAt = diary.getCreatedAt();
        this.dayNumber = diary.getDayNumber();
    }
}