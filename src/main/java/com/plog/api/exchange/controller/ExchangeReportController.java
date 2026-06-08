package com.plog.api.exchange.controller;

import com.plog.api.exchange.dto.ReportRequestDto;
import com.plog.api.exchange.dto.ReportResponseDto;
import com.plog.api.exchange.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange/reports")
public class ExchangeReportController {

    private final ReportService reportService;

    // 신고
    @PostMapping
    public ResponseEntity<ReportResponseDto> createReport(@RequestBody ReportRequestDto request) {
        return ResponseEntity.ok(reportService.createReport(request));
    }
}