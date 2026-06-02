package com.plog.api.domain.recommend.dto;

import jakarta.validation.constraints.NotNull;

public record LocationRequest(
        @NotNull Double latitude,
        @NotNull Double longitude,
        String source
) {}
