package com.plog.api.domain.cache;

import com.plog.api.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "image_analysis_cache",
    uniqueConstraints = @UniqueConstraint(name = "uk_iac_sha256", columnNames = "sha256"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageAnalysisCache extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Lob
    @Column(name = "vision_json", nullable = false, columnDefinition = "LONGTEXT")
    private String visionJson;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Builder
    private ImageAnalysisCache(String sha256, String visionJson, String modelVersion) {
        this.sha256 = sha256;
        this.visionJson = visionJson;
        this.modelVersion = modelVersion;
    }

    public void updateVision(String visionJson, String modelVersion) {
        this.visionJson = visionJson;
        this.modelVersion = modelVersion;
    }
}
