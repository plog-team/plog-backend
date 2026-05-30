package com.example.demo.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_match")
@Getter
@NoArgsConstructor
public class ExchangeMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;

    private LocalDateTime createdAt;

    public ExchangeMatch(String status, LocalDateTime createdAt) {
        this.status = status;
        this.createdAt = createdAt;
    }

    public void updateStatus(String status) {
        this.status = status;
    }
}