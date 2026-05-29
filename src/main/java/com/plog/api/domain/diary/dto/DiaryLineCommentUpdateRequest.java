package com.plog.api.domain.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiaryLineCommentUpdateRequest(
        @NotBlank @Size(max = 1000) String content
) {
}
