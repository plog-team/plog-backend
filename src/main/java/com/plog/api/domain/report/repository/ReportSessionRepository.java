package com.plog.api.domain.report.repository;

import com.plog.api.domain.report.entity.ReportSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportSessionRepository extends JpaRepository<ReportSession, Long> {
    Optional<ReportSession> findByThreadId(String threadId);
}
