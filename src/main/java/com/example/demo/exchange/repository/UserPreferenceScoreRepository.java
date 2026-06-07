package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.UserPreferenceScore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserPreferenceScoreRepository extends JpaRepository<UserPreferenceScore, Long> {
    List<UserPreferenceScore> findByUserId(Long userId);
}