package com.plog.api.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SignInRequestDto {
    @NotBlank private String name;
    @NotBlank private String userPassword;
}