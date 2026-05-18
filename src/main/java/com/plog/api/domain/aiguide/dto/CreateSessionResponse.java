package com.plog.api.domain.aiguide.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record CreateSessionResponse(
    Long sessionId,
    String status,
    String mode,
    /** BATCH 모드일 때만 채워짐 */
    List<GuideQuestionDto> questions,
    /** CONVERSATION 모드일 때 첫 AI 발화 */
    String firstAssistantMessage,
    /** 사진 분석 결과 (양 모드 공통) */
    List<PhotoAnalysisDto> photos
) {}
