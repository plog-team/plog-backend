package com.plog.api.domain.aiguide.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * [Day 8.10] satisfactionScore nullable로 완화 — 별점 UI 제거.
 * 클라이언트는 null 또는 미전송 가능. 값을 보내면 1~5 범위 검증만 적용.
 * comment가 본 피드백의 실제 학습 입력.
 */
public record FeedbackRequest(
    @Min(value = 1, message = "satisfactionScore는 1~5 범위입니다")
    @Max(value = 5, message = "satisfactionScore는 1~5 범위입니다")
    Integer satisfactionScore,

    @Size(max = 1000)
    String comment
) {}
