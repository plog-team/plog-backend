package com.example.demo.exchange.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class MatchRecommendResponseDto {
    private Long userId;
    private String nickname;
    private double similarityScore;
    private List<String> topCategories;

    public MatchRecommendResponseDto(Long userId, String nickname, double similarityScore, List<String> topCategories) {
        this.userId = userId;
        this.nickname = nickname;
        this.similarityScore = similarityScore;
        this.topCategories = topCategories;
    }
}