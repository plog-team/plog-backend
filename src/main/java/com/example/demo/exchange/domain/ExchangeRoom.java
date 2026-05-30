package com.example.demo.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_room")
@Getter
@NoArgsConstructor
public class ExchangeRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private ExchangeMatch exchangeMatch;

    private String status;

    private LocalDateTime createdAt;

    public ExchangeRoom(ExchangeMatch exchangeMatch, String status, LocalDateTime createdAt) {
        this.exchangeMatch = exchangeMatch;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void close() {
        this.status = "CLOSED";
    }
}