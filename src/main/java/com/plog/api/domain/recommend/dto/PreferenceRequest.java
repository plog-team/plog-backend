package com.plog.api.domain.recommend.dto;

import java.util.List;

public record PreferenceRequest(List<String> preferredCategories) {}
