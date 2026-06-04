package com.plog.api.domain.recommend.dto;

import com.plog.api.domain.recommend.UserPreference;
import java.time.LocalDateTime;
import java.util.List;

public record PreferenceResponse(
        Long userId,
        List<String> preferredCategories,
        LocalDateTime updatedAt
) {
    public static PreferenceResponse from(UserPreference p) {
        return new PreferenceResponse(
                p.getUserId(), p.getCategoryList(), p.getUpdatedAt());
    }
}
