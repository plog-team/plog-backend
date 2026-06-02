package com.plog.api.domain.auth;

import com.plog.api.domain.auth.dto.SignInRequestDto;
import com.plog.api.domain.auth.dto.SignInResponseDto;
import com.plog.api.domain.auth.dto.SignUpRequestDto;

public interface AuthService {
    void signUp(SignUpRequestDto dto);
    SignInResponseDto signIn(SignInRequestDto dto);
}