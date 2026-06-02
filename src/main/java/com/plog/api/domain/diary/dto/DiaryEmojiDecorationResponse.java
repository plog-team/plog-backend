package com.plog.api.domain.diary.dto;

import java.time.LocalDateTime;

import com.plog.api.domain.diary.DiaryEmojiDecoration;

public record DiaryEmojiDecorationResponse(
        Long decorationId,
        Long diaryId,
        Long userId,
        String authorName,
        String emoji,
        Double xRatio,
        Double yRatio,
        Double scale,
        Double rotation,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DiaryEmojiDecorationResponse from(DiaryEmojiDecoration decoration, String authorName) {
        return new DiaryEmojiDecorationResponse(
                decoration.getId(),
                decoration.getDiaryId(),
                decoration.getUserId(),
                authorName,
                decoration.getEmoji(),
                decoration.getXRatio(),
                decoration.getYRatio(),
                decoration.getScale(),
                decoration.getRotation(),
                decoration.getCreatedAt(),
                decoration.getUpdatedAt());
    }
}
