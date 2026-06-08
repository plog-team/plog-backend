package com.plog.api.exchange.service;

import com.plog.api.exchange.domain.AppUser;
import com.plog.api.exchange.domain.UserPreferenceScore;
import com.plog.api.exchange.dto.PreferenceUpdateRequest;
import com.plog.api.exchange.repository.AppUserRepository;
import com.plog.api.exchange.repository.UserPreferenceScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final AppUserRepository userRepository;
    private final UserPreferenceScoreRepository userPreferenceScoreRepository;

    @Transactional
    public void update(Long userId, PreferenceUpdateRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> categories = Arrays.stream(request.preferredCategories().split(","))
                .map(String::trim)
                .toList();

        for (String category : categories) {
            Float score = request.categoryScores() != null
                    ? request.categoryScores().getOrDefault(category, 0f)
                    : 0f;

            userPreferenceScoreRepository.findByUserAndCategory(user, category)
                    .ifPresentOrElse(
                            existing -> {
                                existing.setScore(score);
                                existing.setUpdatedAt(LocalDateTime.now());
                            },
                            () -> userPreferenceScoreRepository.save(
                                    UserPreferenceScore.builder()
                                            .user(user)
                                            .category(category)
                                            .score(score)
                                            .updatedAt(LocalDateTime.now())
                                            .build()
                            )
                    );
        }
    }
}
