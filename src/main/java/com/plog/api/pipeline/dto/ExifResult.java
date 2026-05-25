package com.plog.api.pipeline.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record ExifResult(
    LocalDateTime capturedAt,
    Double latitude,
    Double longitude,
    String cameraMake,
    String cameraModel,
    Integer isoSpeed,
    String exposureTime,
    Integer pixelWidth,
    Integer pixelHeight
) {
    public static ExifResult empty() {
        return ExifResult.builder().build();
    }
}
