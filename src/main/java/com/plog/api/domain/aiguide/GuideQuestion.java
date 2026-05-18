package com.plog.api.domain.aiguide;

import com.plog.api.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "guide_question", indexes = {
    @Index(name = "idx_gq_session", columnList = "session_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "order_idx", nullable = false)
    private Integer orderIdx;

    @Column(nullable = false, length = 500)
    private String question;

    /** 가이드 질문의 유형 분류. */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 16)
    private QuestionType questionType;

    /** Gemini가 제안한 답변 후보 3개 (JSON 직렬화). */
    @Lob
    @Column(name = "suggested_answers_json", columnDefinition = "LONGTEXT")
    private String suggestedAnswersJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String answer;

    @Builder
    private GuideQuestion(Long sessionId, Integer orderIdx, String question,
                          QuestionType questionType, String suggestedAnswersJson, String answer) {
        this.sessionId = sessionId;
        this.orderIdx = orderIdx;
        this.question = question;
        this.questionType = questionType;
        this.suggestedAnswersJson = suggestedAnswersJson;
        this.answer = answer;
    }

    public void updateAnswer(String answer) {
        this.answer = answer;
    }
}
