package com.plog.api.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_participant")
@Getter
@NoArgsConstructor
public class MatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private ExchangeMatch exchangeMatch;

    @Column(name = "user_id")
    private Long userId;

    public MatchParticipant(ExchangeMatch exchangeMatch, Long userId) {
        this.exchangeMatch = exchangeMatch;
        this.userId = userId;
    }
}