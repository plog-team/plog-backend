package com.plog.api.exchange.dto;

import java.util.Map;

public record PreferenceUpdateRequest(
        String preferredCategories,
        Map<String, Float> categoryScores
) {}
