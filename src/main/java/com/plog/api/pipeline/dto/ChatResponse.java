package com.plog.api.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record ChatResponse(
    String text,
    @JsonProperty("ready_for_draft") boolean readyForDraft
) {}
