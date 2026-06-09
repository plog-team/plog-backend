package com.plog.api.domain.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.plog.api.domain.auth.dto.EmailRequest;
import com.plog.api.domain.auth.dto.SignInRequestDto;
import com.plog.api.domain.auth.dto.SignInResponseDto;
import com.plog.api.domain.auth.dto.SignUpRequestDto;
import com.plog.api.domain.auth.dto.VerifyRequest;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationRepository repository;
    private final MailService mailService;

    public String createCode() {
        return String.format("%06d",
                new Random().nextInt(1000000));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> signUp(@RequestBody @Valid SignUpRequestDto dto) {
        authService.signUp(dto);
        return ResponseEntity.ok().build(); 
    }

    @PostMapping("/login")
    public SignInResponseDto signIn(@RequestBody @Valid SignInRequestDto dto) {
        return authService.signIn(dto); 
    }

    @PostMapping("/email/send")
    public ResponseEntity<Void> sendCode(@RequestBody EmailRequest request) {

        repository.findByEmail(request.getEmail())
                .ifPresent(repository::delete);

        String code = createCode();

        // 메일 발송 
        mailService.sendCode(request.getEmail(), code);

        // 저장
        EmailVerification verification = new EmailVerification();
        verification.setEmail(request.getEmail());
        verification.setCode(code);
        verification.setVerified(false);
        verification.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        repository.save(verification);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/verify")
    public ResponseEntity<Void> verify(
            @RequestBody VerifyRequest request
    ) {

        EmailVerification verification =
                repository.findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException("인증 요청 내역이 없습니다."));

        if (verification.getExpiredAt()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "인증코드가 만료되었습니다.");
        }

        if (!verification.getCode()
                .equals(request.getVerificationCode())) {

            throw new RuntimeException(
                    "인증코드가 일치하지 않습니다.");
        }

        verification.setVerified(true);

        repository.save(verification);

        return ResponseEntity.ok().build();
    }
}