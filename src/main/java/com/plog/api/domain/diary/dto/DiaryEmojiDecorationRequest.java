package com.plog.api.domain.diary.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiaryEmojiDecorationRequest(
        @NotBlank @Size(max = 20) String emoji,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double xRatio,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double yRatio,
        @NotNull @DecimalMin("0.2") @DecimalMax("5.0") Double scale,
        Double rotation
) {
}
