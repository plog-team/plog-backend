package com.plog.api.domain.aiguide.dto;

import lombok.Builder;

@Builder
public record SendChatResponse(
    String assistantMessage,
    boolean readyForDraft,
    Long userMessageId,
    Long assistantMessageId,
    long latencyMs
) {}
