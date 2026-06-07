package com.example.demo.exchange.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExchangeDiaryRequestDto {

    private Long sessionId;
    private Long userId;
    private String content;
    private int dayNumber;
}