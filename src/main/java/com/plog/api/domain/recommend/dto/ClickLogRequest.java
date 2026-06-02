package com.plog.api.domain.recommend.dto;

import jakarta.validation.constraints.NotBlank;

public record ClickLogRequest(
        @NotBlank String contentId,
        String contentTypeId,
        String category
) {}
