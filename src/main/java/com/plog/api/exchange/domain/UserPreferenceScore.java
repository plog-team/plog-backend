package com.plog.api.exchange.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_preference_score",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    private String category;

    @Setter
    private float score;

    @Setter
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
