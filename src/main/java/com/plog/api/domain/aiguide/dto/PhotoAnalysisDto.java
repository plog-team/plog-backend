package com.plog.api.domain.aiguide.dto;

import com.plog.api.pipeline.dto.ExifResult;
import com.plog.api.pipeline.dto.VisionResult;

import lombok.Builder;

@Builder
public record PhotoAnalysisDto(
    Long photoId,
    String sha256,
    ExifResult exif,
    VisionResult vision,
    boolean cacheHit,
    long latencyMs
) {}
