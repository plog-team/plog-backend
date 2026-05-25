package com.plog.api.domain.aiguide.dto;

import java.time.LocalDateTime;

import com.plog.api.domain.aiguide.AiSession;

import lombok.Builder;

/**
 * [Day 8.6 B] GET /sessions?userId=... 응답용 세션 요약.
 * 명세 §6의 "대화 검색 기능"용으로 본문은 포함하지 않고 메타만.
 */
@Builder
public record SessionSummaryDto(
    Long sessionId,
    String status,
    String mode,
    String persona,
    String photoIdsCsv,
    Integer draftCharCount,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {
    public static SessionSummaryDto from(AiSession s) {
        int draftLen = s.getDraft() == null ? 0 : s.getDraft().length();
        return SessionSummaryDto.builder()
                .sessionId(s.getId())
                .status(s.getStatus().name())
                .mode(s.getMode().name())
                .persona(s.getPersona().name())
                .photoIdsCsv(s.getPhotoIdsCsv())
                .draftCharCount(draftLen)
                .createdAt(s.getCreatedAt())
                .completedAt(s.getCompletedAt())
                .build();
    }
}
