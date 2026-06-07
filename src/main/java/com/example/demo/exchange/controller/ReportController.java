package com.example.demo.exchange.controller;

import com.example.demo.exchange.dto.ReportRequestDto;
import com.example.demo.exchange.dto.ReportResponseDto;
import com.example.demo.exchange.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange/reports")
public class ReportController {

    private final ReportService reportService;

    // 신고
    @PostMapping
    public ResponseEntity<ReportResponseDto> createReport(@RequestBody ReportRequestDto request) {
        return ResponseEntity.ok(reportService.createReport(request));
    }
}