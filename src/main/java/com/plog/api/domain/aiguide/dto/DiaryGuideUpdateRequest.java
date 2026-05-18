package com.plog.api.domain.aiguide.dto;

import jakarta.validation.constraints.Size;

public record DiaryGuideUpdateRequest(
    @Size(max = 4000, message = "guideMd는 최대 4000자입니다")
    String guideMd
) {}
