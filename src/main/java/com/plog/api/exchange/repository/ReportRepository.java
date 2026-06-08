package com.plog.api.exchange.repository;

import com.plog.api.exchange.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}