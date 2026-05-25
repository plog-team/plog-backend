package com.plog.api.domain.aiguide.dto;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.domain.aiguide.GuideQuestion;
import com.plog.api.domain.aiguide.QuestionType;

import lombok.Builder;

@Builder
public record GuideQuestionDto(
    Long questionId,
    Integer orderIdx,
    String question,
    QuestionType type,
    /** [Day 8.11] Gemini가 제안한 답변 후보 3개 — Chip UX */
    List<String> suggestedAnswers,
    String answer
) {
    /** [Day 8.11] suggestedAnswersJson 역직렬화 위한 공유 ObjectMapper (Spring bean과 별개로 안전한 static 사용). */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    public static GuideQuestionDto from(GuideQuestion q) {
        return GuideQuestionDto.builder()
                .questionId(q.getId())
                .orderIdx(q.getOrderIdx())
                .question(q.getQuestion())
                .type(q.getQuestionType())
                .suggestedAnswers(parseSuggestedAnswers(q.getSuggestedAnswersJson()))
                .answer(q.getAnswer())
                .build();
    }

    private static List<String> parseSuggestedAnswers(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, STRING_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }
}
