package com.plog.api.domain.aichat;



import com.plog.api.domain.user.User;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_context")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String recentTopics;

    @Column(columnDefinition = "TEXT")
    private String preferredCategories;

    @Column(length = 50)
    private String lastEmotion;

    @Column(length = 30)
    private String emotionTrend;

    private Double moodScore;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private AiChatContext(User user) {
        this.user = user;
        this.updatedAt = LocalDateTime.now();
    }

    public static AiChatContext of(User user) {
        return AiChatContext.builder().user(user).build();
    }

    public void update(String lastEmotion, String emotionTrend, Double moodScore, String recentTopics) {
        this.lastEmotion = lastEmotion;
        this.emotionTrend = emotionTrend;
        this.moodScore = moodScore;
        this.recentTopics = recentTopics;
        this.updatedAt = LocalDateTime.now();
    }
}