package com.plog.api.domain.aiguide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendChatRequest(
    @NotBlank(message = "message는 비어있을 수 없습니다")
    @Size(max = 2000, message = "message는 최대 2000자입니다")
    String message
) {}
