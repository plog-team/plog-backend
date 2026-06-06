package com.plog.api.domain.diary.dto;

import java.time.LocalDateTime;

import com.plog.api.domain.diary.DiaryLineComment;

public record DiaryLineCommentResponse(
        Long commentId,
        Long diaryId,
        Long userId,
        String authorName,
        Integer lineIndex,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DiaryLineCommentResponse from(DiaryLineComment comment, String authorName) {
        return new DiaryLineCommentResponse(
                comment.getId(),
                comment.getDiaryId(),
                comment.getUserId(),
                authorName,
                comment.getLineIndex(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
