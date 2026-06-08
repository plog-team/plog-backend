package com.plog.api.domain.recommend.dto;

import lombok.Builder;

@Builder
public record PlaceItemDto(
        String contentId,
        String title,
        String address,
        String imageUrl,
        String contentTypeId,
        String dist,
        double lat,
        double lng
) {}