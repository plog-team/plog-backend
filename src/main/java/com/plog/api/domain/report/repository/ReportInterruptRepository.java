package com.plog.api.domain.report.repository;

import com.plog.api.domain.report.entity.ReportInterrupt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReportInterruptRepository extends JpaRepository<ReportInterrupt, Long> {
    List<ReportInterrupt> findBySessionIdOrderByOrderIdxAsc(Long sessionId);
    Optional<ReportInterrupt> findFirstBySessionIdAndAnswerIsNullOrderByOrderIdxAsc(Long sessionId);
    boolean existsBySessionIdAndAnswerIsNull(Long sessionId);
}
