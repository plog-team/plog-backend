package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.Report;
import com.example.demo.exchange.dto.ReportRequestDto;
import com.example.demo.exchange.dto.ReportResponseDto;
import com.example.demo.exchange.repository.ReportRepository;
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