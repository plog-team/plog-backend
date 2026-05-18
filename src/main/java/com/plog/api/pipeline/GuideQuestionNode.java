package com.plog.api.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.plog.api.domain.aiguide.QuestionType;
import com.plog.api.pipeline.dto.BatchQuestion;
import com.plog.api.pipeline.dto.BatchQuestionsResponse;
import com.plog.api.pipeline.dto.VisionResult;

/**
 * BATCH 모드 — Gemini 호출 실패 시 결정론 질문으로 fallback.
 * vision 결과를 직접 prefix하지 않아 어색한 조사·문장형 scene 삽입 문제를 방지.
 */
@Component
public class GuideQuestionNode {

    /**
     * Gemini 실패 시 결정론 질문 N개 + 답변 후보 3개 반환.
     * vision 결과를 질문에 직접 삽입하지 않아 자연스러운 문장 유지.
     */
    public BatchQuestionsResponse generateFallback(VisionResult vision, int count) {
        List<BatchQuestion> pool = new ArrayList<>();
        pool.add(new BatchQuestion(
                "사진 속 자리에 누구와 함께였나요? 혼자였다면 그 자리에서 어떤 생각이 들었어요?",
                QuestionType.SITUATION,
                List.of("친구랑 같이 갔다.", "혼자 잠시 다녀왔다.", "가족이랑 함께한 시간이었다.")));
        pool.add(new BatchQuestion(
                "이 자리를 찾게 된 특별한 이유가 있었나요?",
                QuestionType.MEANING,
                List.of("오랜만에 시간이 나서 들렀다.", "마침 마음이 가는 곳이라 들렀다.", "약속이 있어 자연스럽게 들렀다.")));
        pool.add(new BatchQuestion(
                "사진을 찍던 순간 가장 인상 깊었던 한 가지는 무엇인가요?",
                QuestionType.EMOTION,
                List.of("주변 분위기가 좋았다.", "함께 있던 사람들이 좋았다.", "공간 자체가 인상 깊었다.")));
        pool.add(new BatchQuestion(
                "이 사진을 다시 보니 어떤 기억이 가장 먼저 떠오르세요?",
                QuestionType.MEANING,
                List.of("함께 나눈 대화가 떠오른다.", "별 생각 없이 지나갔던 한 장면이 다시 보인다.", "비슷한 자리에서의 다른 기억이 떠오른다.")));
        pool.add(new BatchQuestion(
                "사진 속 시간을 한 단어로 표현한다면 어떤 단어가 어울릴까요?",
                QuestionType.EMOTION,
                List.of("편안함이 어울린다.", "설렘이 어울린다.", "잔잔함이 어울린다.")));
        pool.add(new BatchQuestion(
                "하루의 흐름에서 가장 천천히 흘러간 시간은 언제였나요?",
                QuestionType.MEANING,
                List.of("앉아서 쉬던 시간이었다.", "음식을 기다리던 시간이었다.", "사진을 한참 들여다보던 시간이었다.")));
        pool.add(new BatchQuestion(
                "하루를 한 문장으로 정리한다면 어떤 문장이 떠올라요?",
                QuestionType.MEANING,
                List.of("조용히 채워진 하루였다.", "오랜만에 마음이 가벼웠다.", "예상보다 알찬 하루였다.")));
        pool.add(new BatchQuestion(
                "이날 사진으로 가장 남기고 싶었던 한 장면은 어떤 것이었나요?",
                QuestionType.SITUATION,
                List.of("음식이 나오던 장면을 남기고 싶었다.", "창밖 풍경이 인상 깊었다.", "마주 앉은 자리가 좋았다.")));

        int n = Math.max(1, Math.min(count, pool.size()));
        return new BatchQuestionsResponse(new ArrayList<>(pool.subList(0, n)));
    }
}
