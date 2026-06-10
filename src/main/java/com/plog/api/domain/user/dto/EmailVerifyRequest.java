package com.plog.api.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class EmailVerifyRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String verificationCode;
}
