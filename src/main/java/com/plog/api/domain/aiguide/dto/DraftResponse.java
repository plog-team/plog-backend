package com.plog.api.domain.aiguide.dto;

import lombok.Builder;

@Builder
public record DraftResponse(
    Long sessionId,
    String draft,
    Integer charCount
) {}
