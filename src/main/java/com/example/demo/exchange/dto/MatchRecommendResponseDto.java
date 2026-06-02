package com.example.demo.exchange.dto;

import lombok.Getter;

@Getter
public class MatchRecommendResponseDto {
    private Long userId;
    private String nickname;
    private double similarityScore;

    public MatchRecommendResponseDto(Long userId, String nickname, double similarityScore) {
        this.userId = userId;
        this.nickname = nickname;
        this.similarityScore = similarityScore;
    }
}