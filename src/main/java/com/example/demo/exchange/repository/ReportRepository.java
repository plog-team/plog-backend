package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}