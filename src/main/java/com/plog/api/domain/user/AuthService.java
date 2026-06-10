package com.plog.api.domain.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plog.api.common.exception.BadRequestException;
import com.plog.api.config.JwtProvider;
import com.plog.api.domain.user.dto.AuthResponse;
import com.plog.api.domain.user.dto.LoginRequest;
import com.plog.api.domain.user.dto.RegisterRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailVerificationRepository emailVerificationRepository;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        EmailVerification verification = emailVerificationRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadRequestException("이메일 인증을 먼저 진행해주세요"));

        if (!verification.isVerified()) {
            throw new BadRequestException("이메일 인증이 완료되지 않았습니다");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("이미 사용 중인 이메일입니다");
        }

        User user = User.of(req.getName(), req.getEmail(), passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);
        emailVerificationRepository.delete(verification);
        return new AuthResponse(jwtProvider.generate(user.getId()), user.getId());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByName(req.getName())
                .orElseThrow(() -> new BadRequestException("이름 또는 비밀번호가 올바르지 않습니다"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadRequestException("이름 또는 비밀번호가 올바르지 않습니다");
        }
        return new AuthResponse(jwtProvider.generate(user.getId()), user.getId());
    }
}
