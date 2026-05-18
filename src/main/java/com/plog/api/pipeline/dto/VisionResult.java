package com.plog.api.pipeline.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record VisionResult(
    List<String> objects,
    String scene,
    String mood,
    @JsonProperty("time_of_day") String timeOfDay,
    @JsonProperty("weather_hint") String weatherHint,
    @JsonProperty("suggested_emotion") String suggestedEmotion,
    @JsonProperty("one_line_summary") String oneLineSummary
) {}
