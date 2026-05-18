package com.plog.api.domain.photo.dto;

import java.util.List;

/**
 * [Day 8.8 B2] 명세 §6 `POST /api/photos` 배치 응답 — `{photos: [{photoId, url, hash}]}`.
 */
public record PhotoUploadBatchResponse(List<PhotoUploadResponse> photos) {}
