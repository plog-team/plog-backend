package com.plog.api.domain.diary.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.plog.api.domain.diary.Diary;

public record DiaryResponse(
        Long diaryId,
        LocalDate date,
        String title,
        String body,
        String location,
        String weather,
        boolean secret,
        boolean bookmarked,
        int representativePhotoIndex,
        List<Long> photoIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DiaryResponse from(Diary diary) {
        return new DiaryResponse(
                diary.getId(),
                diary.getDiaryDate(),
                diary.getTitle(),
                diary.getBody(),
                diary.getLocation(),
                diary.getWeather(),
                diary.isSecret(),
                diary.isBookmarked(),
                diary.getRepresentativePhotoIndex(),
                parsePhotoIds(diary.getPhotoIdsCsv()),
                diary.getCreatedAt(),
                diary.getUpdatedAt());
    }

    private static List<Long> parsePhotoIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .toList();
    }
}
