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
    /** 감정 값이 아직 없을 때 사용하는 기본 변환 */
    public static DiarySearchResponse from(Diary diary, String imageUrl) {
        return from(diary, null, imageUrl);
    }

    /** 감정 분석 테이블에서 조회한 감정을 포함해서 변환 */
    public static DiarySearchResponse from(Diary diary, String emotion, String imageUrl) {
        return new DiarySearchResponse(
                diary.getId(),
                diary.getDiaryDate(),
                emotion,
                diary.getTitle(),
                diary.getBody(),
                diary.getLocation(),
                imageUrl
        );
    }
}