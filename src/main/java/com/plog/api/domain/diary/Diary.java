package com.plog.api.domain.diary;

import java.time.LocalDate;

import com.plog.api.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "diary", indexes = {
    @Index(name = "idx_diary_user_date", columnList = "user_id, diary_date"),
    @Index(name = "idx_diary_user_created", columnList = "user_id, created_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_diary_user_date", columnNames = {"user_id", "diary_date"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @Column(nullable = false, length = 120)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String body;

    @Column(length = 120)
    private String location;

    @Column(length = 80)
    private String weather;

    @Column(nullable = false)
    private boolean secret;

    @Column(nullable = false)
    private boolean bookmarked;

    @Column(name = "representative_photo_index", nullable = false)
    private int representativePhotoIndex;

    @Column(name = "photo_ids_csv", length = 500)
    private String photoIdsCsv;

    @Builder
    private Diary(Long userId, LocalDate diaryDate, String title, String body, String location,
                  String weather, boolean secret, boolean bookmarked, int representativePhotoIndex,
                  String photoIdsCsv) {
        this.userId = userId;
        this.diaryDate = diaryDate;
        update(diaryDate, title, body, location, weather, secret, bookmarked,
                representativePhotoIndex, photoIdsCsv);
    }

    public void update(LocalDate diaryDate, String title, String body, String location,
                       String weather, boolean secret, boolean bookmarked,
                       int representativePhotoIndex, String photoIdsCsv) {
        this.diaryDate = diaryDate;
        this.title = title;
        this.body = body;
        this.location = location;
        this.weather = weather;
        this.secret = secret;
        this.bookmarked = bookmarked;
        this.representativePhotoIndex = Math.max(0, representativePhotoIndex);
        this.photoIdsCsv = photoIdsCsv;
    }
}
