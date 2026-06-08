package com.plog.api.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_participant")
@Getter
@NoArgsConstructor
public class ExchangeParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id")
    private ExchangeRoom exchangeRoom;

    @Column(name = "user_id")
    private Long userId;

    private String role;

    private LocalDateTime joinedAt;
}