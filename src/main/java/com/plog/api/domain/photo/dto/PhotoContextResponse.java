package com.plog.api.domain.photo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.plog.api.domain.photo.PhotoContext;

import lombok.Builder;

@Builder
public record PhotoContextResponse(
        Long photoId,
        LocalDateTime capturedAt,
        LocalDate date,
        Double latitude,
        Double longitude,
        String locationHint,
        String weather,
        Double temperature
) {
    public static PhotoContextResponse from(PhotoContext context) {
        if (context == null) return null;
        LocalDateTime capturedAt = context.getCapturedAt();
        return PhotoContextResponse.builder()
                .photoId(context.getPhotoId())
                .capturedAt(capturedAt)
                .date(capturedAt == null ? null : capturedAt.toLocalDate())
                .latitude(context.getLatitude())
                .longitude(context.getLongitude())
                .locationHint(context.getLocationHint())
                .weather(context.getWeather())
                .temperature(context.getTemperature())
                .build();
    }
}