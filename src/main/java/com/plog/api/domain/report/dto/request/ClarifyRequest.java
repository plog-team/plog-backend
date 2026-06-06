package com.plog.api.domain.report.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ClarifyRequest(@NotBlank String answer) {}
