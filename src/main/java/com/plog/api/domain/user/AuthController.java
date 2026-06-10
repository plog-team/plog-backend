package com.plog.api.domain.user;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plog.api.domain.user.dto.AuthResponse;
import com.plog.api.domain.user.dto.EmailSendRequest;
import com.plog.api.domain.user.dto.EmailVerifyRequest;
import com.plog.api.domain.user.dto.LoginRequest;
import com.plog.api.domain.user.dto.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/logout")
    public void logout() {
        // JWT는 stateless — 클라이언트에서 토큰 삭제로 로그아웃 처리
    }

    @PostMapping("/email/send")
    public void sendVerificationEmail(@Valid @RequestBody EmailSendRequest req) {
        emailVerificationService.send(req.getEmail());
    }

    @PostMapping("/email/verify")
    public void verifyEmail(@Valid @RequestBody EmailVerifyRequest req) {
        emailVerificationService.verify(req.getEmail(), req.getVerificationCode());
    }
}
