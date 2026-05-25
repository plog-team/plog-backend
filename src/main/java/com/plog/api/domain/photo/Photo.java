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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "photo", indexes = {
    @Index(name = "idx_photo_user", columnList = "user_id"),
    @Index(name = "idx_photo_sha256", columnList = "sha256")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "stored_path", nullable = false, length = 500)
    private String storedPath;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Builder
    private Photo(Long userId, String sha256, String originalFilename, String mimeType,
                  Integer width, Integer height, Long sizeBytes, String storedPath,
                  LocalDateTime capturedAt) {
        this.userId = userId;
        this.sha256 = sha256;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.storedPath = storedPath;
        this.capturedAt = capturedAt;
    }
}
