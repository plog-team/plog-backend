package com.example.demo.exchange.dto;

import com.example.demo.exchange.domain.ExchangeRoom;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ExchangeRoomResponseDto {

    private Long id;
    private Long matchId;
    private String status;
    private LocalDateTime createdAt;

    public ExchangeRoomResponseDto(ExchangeRoom room) {
        this.id = room.getId();
        this.matchId = room.getExchangeMatch() != null ? room.getExchangeMatch().getId() : null;
        this.status = room.getStatus();
        this.createdAt = room.getCreatedAt();
    }
}