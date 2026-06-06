package com.plog.api.domain.photo.dto;

import lombok.Builder;

@Builder
public record PhotoUploadResponse(
    Long photoId,
    String sha256,
    String originalFilename,
    String mimeType,
    Integer width,
    Integer height,
    Long sizeBytes,
    String storedPath,
    boolean cacheHit,
    PhotoAutoInputContext context
) {}
