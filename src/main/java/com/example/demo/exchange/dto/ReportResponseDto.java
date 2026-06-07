package com.example.demo.exchange.dto;

import com.example.demo.exchange.domain.Report;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportResponseDto {
    private Long id;
    private Long reporterId;
    private Long reportedId;
    private String reason;
    private LocalDateTime createdAt;

    public ReportResponseDto(Report report) {
        this.id = report.getId();
        this.reporterId = report.getReporterId();
        this.reportedId = report.getReportedId();
        this.reason = report.getReason();
        this.createdAt = report.getCreatedAt();
    }
}