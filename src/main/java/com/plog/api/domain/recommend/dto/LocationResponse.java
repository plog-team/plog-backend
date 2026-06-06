package com.plog.api.domain.recommend.dto;

import com.plog.api.domain.recommend.UserLocationLog;
import java.time.LocalDateTime;

public record LocationResponse(
        Double latitude,
        Double longitude,
        String source,
        LocalDateTime createdAt
) {
    public static LocationResponse from(UserLocationLog l) {
        return new LocationResponse(
                l.getLatitude(), l.getLongitude(),
                l.getSource(), l.getCreatedAt());
    }
}
