package com.plog.api.domain.aiguide;

import java.time.LocalDateTime;

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
@Table(name = "ai_session", indexes = {
    @Index(name = "idx_ai_session_user", columnList = "user_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiSession extends BaseTimeEntity {

    public enum Status { ACTIVE, COMPLETED, ABANDONED }
    public enum Mode { BATCH, CONVERSATION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Persona persona;

    @Column(name = "photo_ids_csv", length = 500)
    private String photoIdsCsv;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String draft;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    private AiSession(Long userId, Status status, Mode mode, Persona persona, String photoIdsCsv) {
        this.userId = userId;
        this.status = status == null ? Status.ACTIVE : status;
        this.mode = mode == null ? Mode.BATCH : mode;
        this.persona = persona == null ? Persona.DEFAULT : persona;
        this.photoIdsCsv = photoIdsCsv;
    }

    public void updateDraft(String draft) {
        this.draft = draft;
    }

    public void complete() {
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void abandon() {
        this.status = Status.ABANDONED;
    }
}
