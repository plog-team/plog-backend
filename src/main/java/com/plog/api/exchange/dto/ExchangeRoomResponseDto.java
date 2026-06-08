package com.plog.api.exchange.dto;

import com.plog.api.exchange.domain.ExchangeRoom;
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