package com.example.demo.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "exchange_session")
@Getter
@NoArgsConstructor
public class ExchangeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id")
    private ExchangeRoom exchangeRoom;

    private LocalDate startDate;

    private LocalDate endDate;

    private String status;

    private boolean isExtended;

    public ExchangeSession(ExchangeRoom exchangeRoom, LocalDate startDate, String status) {
        this.exchangeRoom = exchangeRoom;
        this.startDate = startDate;
        this.endDate = startDate.plusDays(7);
        this.status = status;
        this.isExtended = false;
    }

    public void end(LocalDate endDate) {
        this.endDate = endDate;
        this.status = "CLOSED";
    }

    public void extend() {
        this.endDate = this.endDate.plusDays(7);
        this.isExtended = true;
    }
}