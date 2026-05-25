package com.plog.api.pipeline.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import com.plog.api.domain.aiguide.QuestionType;

/**
 * [Day 8.11] BATCH 모드 — Gemini가 생성한 질문 1개 + 답변 후보 3개.
 * Gemini JSON 응답은 snake_case suggested_answers를 사용하므로 @JsonAlias로 양쪽 모두 허용.
 */
public record BatchQuestion(
    String text,
    QuestionType type,
    @JsonAlias({"suggested_answers", "suggestedAnswers"})
    List<String> suggestedAnswers
) {}
