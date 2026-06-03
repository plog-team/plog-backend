package com.plog.api.domain.photo;

import java.time.LocalDateTime;

import com.plog.api.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "photo_context", indexes = {
        @Index(name = "idx_photo_context_photo", columnList = "photo_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_photo_context_photo", columnNames = "photo_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoContext extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "photo_id", nullable = false)
    private Long photoId;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "location_hint", length = 255)
    private String locationHint;

    @Column(length = 100)
    private String weather;

    @Column
    private Double temperature;

    @Builder
    private PhotoContext(Long photoId, LocalDateTime capturedAt, Double latitude, Double longitude,
                         String locationHint, String weather, Double temperature) {
        this.photoId = photoId;
        this.capturedAt = capturedAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationHint = locationHint;
        this.weather = weather;
        this.temperature = temperature;
    }
}