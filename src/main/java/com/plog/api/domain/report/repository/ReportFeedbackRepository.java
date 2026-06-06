package com.plog.api.domain.report.repository;

import com.plog.api.domain.report.entity.ReportFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportFeedbackRepository extends JpaRepository<ReportFeedback, Long> {}
