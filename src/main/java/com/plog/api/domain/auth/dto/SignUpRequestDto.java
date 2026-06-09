package com.plog.api.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SignUpRequestDto {
    @NotBlank private String userName;
    @NotBlank @Email private String email;
    @NotBlank private String userPassword;
}