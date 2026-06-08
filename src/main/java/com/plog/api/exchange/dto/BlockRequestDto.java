package com.plog.api.exchange.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BlockRequestDto {
    private Long blockerId;
    private Long blockedId;
}