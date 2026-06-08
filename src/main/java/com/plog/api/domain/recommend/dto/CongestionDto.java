package com.plog.api.domain.recommend.dto;

import lombok.Builder;

@Builder
public record CongestionDto(
        String placeName,
        String congestionLevel
) {}