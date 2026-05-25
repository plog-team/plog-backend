package com.plog.api.domain.aiguide;

import com.plog.api.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "user_feedback", indexes = {
    @Index(name = "idx_uf_session", columnList = "session_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** nullable. 텍스트 코멘트만 학습 가이드에 반영. */
    @Column(name = "satisfaction_score", nullable = true)
    private Integer satisfactionScore;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String comment;

    @Builder
    private UserFeedback(Long sessionId, Integer satisfactionScore, String comment) {
        this.sessionId = sessionId;
        this.satisfactionScore = satisfactionScore;
        this.comment = comment;
    }
}
