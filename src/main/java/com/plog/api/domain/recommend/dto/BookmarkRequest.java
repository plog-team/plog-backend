package com.plog.api.domain.recommend.dto;

import jakarta.validation.constraints.NotBlank;

public record BookmarkRequest(
        @NotBlank String contentId,
        @NotBlank String title,
        String address,
        String imageUrl,
        String category,
        String contentTypeId
) {}
