package com.plog.api.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRequest {

    private String email;
    private String verificationCode;

}