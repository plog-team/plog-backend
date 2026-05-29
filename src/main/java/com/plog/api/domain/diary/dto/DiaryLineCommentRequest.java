package com.plog.api.domain.diary.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiaryLineCommentRequest(
        @NotNull @Min(0) Integer lineIndex,
        @NotBlank @Size(max = 1000) String content
) {
}
