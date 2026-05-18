package com.plog.api.domain.aiguide;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideQuestionRepository extends JpaRepository<GuideQuestion, Long> {
    List<GuideQuestion> findAllBySessionIdOrderByOrderIdxAsc(Long sessionId);
}
