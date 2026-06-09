package com.plog.api.domain.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository
        extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmail(String email);
}