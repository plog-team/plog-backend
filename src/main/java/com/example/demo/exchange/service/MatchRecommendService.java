package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.AppUser;
import com.example.demo.exchange.domain.UserPreferenceScore;
import com.example.demo.exchange.dto.MatchRecommendResponseDto;
import com.example.demo.exchange.repository.AppUserRepository;
import com.example.demo.exchange.repository.UserPreferenceScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchRecommendService {

    private final UserPreferenceScoreRepository preferenceRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<MatchRecommendResponseDto> recommendMatches(Long userId) {
        // 내 성향 점수
        Map<String, Double> myScores = preferenceRepository.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(UserPreferenceScore::getCategory, UserPreferenceScore::getScore));

        // 나 제외한 전체 유저
        List<AppUser> otherUsers = appUserRepository.findAll()
                .stream()
                .filter(u -> !u.getId().equals(userId))
                .collect(Collectors.toList());

        // 각 유저와 코사인 유사도 계산
        List<MatchRecommendResponseDto> result = new ArrayList<>();
        for (AppUser user : otherUsers) {
            Map<String, Double> otherScores = preferenceRepository.findByUserId(user.getId())
                    .stream()
                    .collect(Collectors.toMap(UserPreferenceScore::getCategory, UserPreferenceScore::getScore));

            double similarity = cosineSimilarity(myScores, otherScores);
            result.add(new MatchRecommendResponseDto(user.getId(), user.getNickname(), similarity));
        }

        // 유사도 높은 순 정렬
        result.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        return result;
    }

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        Set<String> categories = new HashSet<>(a.keySet());
        categories.retainAll(b.keySet());

        double dotProduct = 0;
        double normA = 0;
        double normB = 0;

        for (String category : categories) {
            dotProduct += a.get(category) * b.get(category);
        }
        for (double v : a.values()) normA += v * v;
        for (double v : b.values()) normB += v * v;

        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}