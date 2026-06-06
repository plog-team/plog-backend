package com.plog.api.domain.aiguide.dto;

import lombok.Builder;

@Builder
public record AnswerResponse(
    GuideQuestionDto answered,
    GuideQuestionDto nextQuestion,
    boolean done,
    int answeredCount,
    int targetCount
) {}
