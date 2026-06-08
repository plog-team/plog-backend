package com.plog.api.exchange.dto;

import com.plog.api.exchange.domain.Block;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BlockResponseDto {
    private Long id;
    private Long blockerId;
    private Long blockedId;
    private LocalDateTime createdAt;

    public BlockResponseDto(Block block) {
        this.id = block.getId();
        this.blockerId = block.getBlockerId();
        this.blockedId = block.getBlockedId();
        this.createdAt = block.getCreatedAt();
    }
}