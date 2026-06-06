package com.plog.api.domain.photo;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "photo_location")
public class PhotoLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long photoId;

    private Double latitude;

    private Double longitude;

    private LocalDateTime takenAt;

    private String locationName;

    private String weather;

    private Double temperature;
}