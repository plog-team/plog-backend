package com.plog.api.exchange.service;

import com.plog.api.exchange.domain.Report;
import com.plog.api.exchange.dto.ReportRequestDto;
import com.plog.api.exchange.dto.ReportResponseDto;
import com.plog.api.exchange.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    @Transactional
    public ReportResponseDto createReport(ReportRequestDto request) {
        Report report = new Report(
                request.getReporterId(),
                request.getReportedId(),
                request.getReason()
        );
        reportRepository.save(report);
        return new ReportResponseDto(report);
    }
}