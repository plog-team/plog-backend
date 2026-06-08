package com.plog.api.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_preference_score")
@Getter
@NoArgsConstructor
public class UserPreferenceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String category;
    private double score;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}