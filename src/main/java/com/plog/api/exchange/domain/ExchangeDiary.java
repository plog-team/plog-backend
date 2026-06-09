package com.plog.api.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_diary")
@Getter
@NoArgsConstructor
public class ExchangeDiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ExchangeSession exchangeSession;

    @Column(name = "user_id")
    private Long userId;

    private String title;

    private String content;

    private LocalDateTime createdAt;

    private int dayNumber;

    public ExchangeDiary(ExchangeSession exchangeSession, Long userId, String title, String content, int dayNumber, LocalDateTime createdAt) {
        this.exchangeSession = exchangeSession;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.dayNumber = dayNumber;
        this.createdAt = createdAt;
    }

    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
    }
}