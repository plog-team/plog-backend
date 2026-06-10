package com.plog.api.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String password;
}
