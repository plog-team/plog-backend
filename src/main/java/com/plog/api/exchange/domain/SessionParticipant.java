package com.plog.api.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_participant")
@Getter
@NoArgsConstructor
public class SessionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ExchangeSession exchangeSession;

    @Column(name = "user_id")
    private Long userId;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    @Column(name = "extend_agreed")
    private boolean extendAgreed;

    public SessionParticipant(ExchangeSession session, Long userId) {
        this.exchangeSession = session;
        this.userId = userId;
        this.joinedAt = LocalDateTime.now();
        this.extendAgreed = false;
    }

    public void agreeExtend() {
        this.extendAgreed = true;
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }
}