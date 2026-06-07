package com.example.demo.exchange.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExchangeMatchRequestDto {

    private Long userId;
    private Long targetUserId;
}