package com.plog.api.domain.aiguide.dto;

import jakarta.validation.constraints.Size;

public record AnswerRequest(
    @Size(max = 2000, message = "answer는 최대 2000자입니다")
    String answer
) {}
