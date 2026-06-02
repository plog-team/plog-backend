package com.plog.api.domain.auth;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendCode(String email, String code) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Plog 이메일 인증");

        message.setText(
                "인증코드 : " + code +
                "\n\n5분 이내에 입력해주세요."
        );

        mailSender.send(message);
    }
}