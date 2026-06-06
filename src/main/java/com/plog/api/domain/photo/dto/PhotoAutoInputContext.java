package com.plog.api.domain.photo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;

/**
 * 사진 EXIF 메타데이터를 기반으로 생성한 일기 자동입력 컨텍스트.
 */
@Builder
public record PhotoAutoInputContext(
        Long photoId,
        LocalDateTime capturedAt,
        LocalDate date,
        Double latitude,
        Double longitude,
        String locationHint,
        String weather,
        Double temperature
) {}