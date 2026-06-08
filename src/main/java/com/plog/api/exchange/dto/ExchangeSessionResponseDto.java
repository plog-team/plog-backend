package com.plog.api.exchange.dto;

import com.plog.api.exchange.domain.ExchangeSession;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class ExchangeSessionResponseDto {

    private Long id;
    private Long exchangeRoomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private boolean isExtended;

    public ExchangeSessionResponseDto(ExchangeSession session) {
        this.id = session.getId();
        this.exchangeRoomId = session.getExchangeRoom().getId();
        this.startDate = session.getStartDate();
        this.endDate = session.getEndDate();
        this.status = session.getStatus();
        this.isExtended = session.isExtended();
    }
}