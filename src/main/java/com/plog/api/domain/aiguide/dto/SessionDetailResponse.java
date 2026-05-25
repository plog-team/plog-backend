package com.plog.api.domain.aiguide.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

@Builder
public record SessionDetailResponse(
    Long sessionId,
    Long userId,
    String status,
    String mode,
    String photoIdsCsv,
    String draft,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    /** BATCH 모드 */
    List<GuideQuestionDto> questions,
    /** CONVERSATION 모드 */
    List<ChatMessageDto> chatMessages
) {}
