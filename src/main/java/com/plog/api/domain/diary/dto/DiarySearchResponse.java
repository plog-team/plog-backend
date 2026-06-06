package com.plog.api.domain.diary.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plog.api.domain.diary.Diary;

/** 검색 화면에서 사용하는 일기 응답 DTO */
public record DiarySearchResponse(
        Long id,

        @JsonProperty("diary_date")
        LocalDate diaryDate,

        String emotion,

        String title,

        String content,

        @JsonProperty("location_name")
        String locationName,

        @JsonProperty("image_url")
        String imageUrl
) {
    public static DiarySearchResponse from(Diary diary, String imageUrl) {
        return new DiarySearchResponse(
                diary.getId(),
                diary.getDiaryDate(),
                null,
                diary.getTitle(),
                diary.getBody(),
                diary.getLocation(),
                imageUrl
        );
    }
}