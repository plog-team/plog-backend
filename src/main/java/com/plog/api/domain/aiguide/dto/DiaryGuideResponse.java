package com.plog.api.domain.aiguide.dto;

import lombok.Builder;

@Builder
public record DiaryGuideResponse(
    Long userId,
    String guideMd,
    Integer lineCount,
    Integer maxLines
) {}
