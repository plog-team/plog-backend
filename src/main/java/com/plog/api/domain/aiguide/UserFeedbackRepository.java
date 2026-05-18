package com.plog.api.domain.aiguide;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
    List<UserFeedback> findAllBySessionId(Long sessionId);
}
