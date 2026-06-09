package com.plog.api.domain.auth;

import com.plog.api.config.JwtProvider;
import com.plog.api.domain.auth.dto.SignInRequestDto;
import com.plog.api.domain.auth.dto.SignInResponseDto;
import com.plog.api.domain.auth.dto.SignUpRequestDto;
import com.plog.api.domain.user.User;
import com.plog.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailVerificationRepository emailVerificationRepository;

    @Override
    public void signUp(SignUpRequestDto dto) {

        EmailVerification verification =
                emailVerificationRepository
                        .findByEmail(dto.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException("이메일 인증을 먼저 진행해주세요."));

        if (!verification.isVerified()) {
            throw new RuntimeException("이메일 인증이 완료되지 않았습니다.");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }

        String encodedPassword =
                passwordEncoder.encode(dto.getUserPassword());

        User user = User.of(
                dto.getUserName(),
                dto.getEmail(),
                encodedPassword
        );

        userRepository.save(user);
    }

    @Override
    public SignInResponseDto signIn(SignInRequestDto dto) {
        User user = userRepository.findByName(dto.getName())
            .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다"));

        if (!passwordEncoder.matches(dto.getUserPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 틀렸습니다");
        }

        String token = jwtProvider.createToken(user.getEmail());
        return new SignInResponseDto(token);
    }
}