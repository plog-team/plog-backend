package com.plog.api.domain.diary.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiaryUpsertRequest(
        @NotNull LocalDate date,
        @NotBlank @Size(max = 120) String title,
        @NotBlank String body,
        @Size(max = 120) String location,
        @Size(max = 80) String weather,
        boolean secret,
        boolean bookmarked,
        int representativePhotoIndex,
        @Size(max = 10) List<Long> photoIds
) {
}
