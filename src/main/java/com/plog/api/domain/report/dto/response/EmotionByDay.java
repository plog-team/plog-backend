package com.plog.api.domain.report.dto.response;

import java.util.List;

public record EmotionByDay(String date, String day, List<String> emotions, boolean hasDiary) {}
