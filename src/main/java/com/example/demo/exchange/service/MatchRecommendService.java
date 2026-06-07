package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.AppUser;
import com.example.demo.exchange.domain.Block;
import com.example.demo.exchange.domain.UserPreferenceScore;
import com.example.demo.exchange.dto.MatchRecommendResponseDto;
import com.example.demo.exchange.repository.AppUserRepository;
import com.example.demo.exchange.repository.BlockRepository;
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
    private final BlockRepository blockRepository;

    @Transactional(readOnly = true)
    public List<MatchRecommendResponseDto> recommendMatches(Long userId) {
        // 내가 차단한 + 나를 차단한 사용자 ID 목록
        Set<Long> excludedIds = new HashSet<>();
        blockRepository.findByBlockerId(userId)
                .forEach(b -> excludedIds.add(b.getBlockedId()));
        blockRepository.findByBlockedId(userId)
                .forEach(b -> excludedIds.add(b.getBlockerId()));

        Map<String, Double> myScores = preferenceRepository.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(UserPreferenceScore::getCategory, UserPreferenceScore::getScore));

        List<AppUser> otherUsers = appUserRepository.findAll()
                .stream()
                .filter(u -> !u.getId().equals(userId))
                .filter(u -> !excludedIds.contains(u.getId()))
                .collect(Collectors.toList());

        List<MatchRecommendResponseDto> result = new ArrayList<>();
        for (AppUser user : otherUsers) {
            List<UserPreferenceScore> otherPreferences = preferenceRepository.findByUserId(user.getId());

            Map<String, Double> otherScores = otherPreferences.stream()
                    .collect(Collectors.toMap(UserPreferenceScore::getCategory, UserPreferenceScore::getScore));

            double similarity = cosineSimilarity(myScores, otherScores);

            List<String> topCategories = otherPreferences.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(3)
                    .map(UserPreferenceScore::getCategory)
                    .collect(Collectors.toList());

            result.add(new MatchRecommendResponseDto(user.getId(), user.getNickname(), similarity, topCategories));
        }

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