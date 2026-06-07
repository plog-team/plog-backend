package com.example.demo.exchange.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequestDto {
    private Long reporterId;
    private Long reportedId;
    private String reason;
}