package com.plog.api.domain.photo.dto;

import com.plog.api.domain.photo.PhotoLocation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PhotoLocationResponse {

    private Long id;
    private Long photoId;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private LocalDateTime takenAt;
    private String weather;
    private Double temperature;

    public static PhotoLocationResponse from(PhotoLocation location) {
        return PhotoLocationResponse.builder()
                .id(location.getId())
                .photoId(location.getPhotoId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .locationName(location.getLocationName())
                .takenAt(location.getTakenAt())
                .weather(location.getWeather())
                .temperature(location.getTemperature())
                .build();
    }
}