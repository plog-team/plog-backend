package com.plog.api.domain.diary;

import com.plog.api.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "diary_emoji_decoration", indexes = {
    @Index(name = "idx_diary_emoji_decoration_diary", columnList = "diary_id"),
    @Index(name = "idx_diary_emoji_decoration_user", columnList = "user_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryEmojiDecoration extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "diary_id", nullable = false)
    private Long diaryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String emoji;

    @Column(name = "x_ratio", nullable = false)
    private Double xRatio;

    @Column(name = "y_ratio", nullable = false)
    private Double yRatio;

    @Column(nullable = false)
    private Double scale;

    @Column(nullable = false)
    private Double rotation;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Builder
    private DiaryEmojiDecoration(Long diaryId, Long userId, String emoji, Double xRatio,
                                 Double yRatio, Double scale, Double rotation) {
        this.diaryId = diaryId;
        this.userId = userId;
        update(emoji, xRatio, yRatio, scale, rotation);
        this.deleted = false;
    }

    public void update(String emoji, Double xRatio, Double yRatio, Double scale, Double rotation) {
        this.emoji = emoji;
        this.xRatio = xRatio;
        this.yRatio = yRatio;
        this.scale = scale;
        this.rotation = rotation == null ? 0.0 : rotation;
    }

    public void delete() {
        this.deleted = true;
    }
}
