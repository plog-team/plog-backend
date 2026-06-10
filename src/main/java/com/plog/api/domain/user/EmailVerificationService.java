package com.plog.api.domain.user;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plog.api.common.exception.BadRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int EXPIRY_MINUTES = 5;

    private final EmailVerificationRepository repository;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${spring.mail.username}")
    private String fromAddress;

    // @Transactional 없음 — JPA save가 먼저 커밋된 뒤 메일 발송
    // @Transactional을 걸면 메일 실패 시 DB 저장까지 롤백되어 코드가 사라짐
    public void send(String email) {
        String code = String.format("%06d", random.nextInt(1_000_000));

        repository.findByEmail(email).ifPresent(repository::delete);

        EmailVerification v = new EmailVerification();
        v.setEmail(email);
        v.setCode(code);
        v.setVerified(false);
        v.setExpiredAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        repository.save(v);  // 메일 발송 전에 먼저 커밋되도록 flush

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[Plog] 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n\n5분 안에 입력해 주세요.");
        mailSender.send(message);

        log.info("[EmailVerification] sent to={}", email);
    }

    @Transactional
    public void verify(String email, String code) {
        EmailVerification v = repository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("인증 요청 내역이 없습니다"));

        if (v.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("인증코드가 만료되었습니다");
        }

        if (!v.getCode().equals(code)) {
            throw new BadRequestException("인증코드가 올바르지 않습니다");
        }

        v.setVerified(true);
        repository.save(v);
    }
}
