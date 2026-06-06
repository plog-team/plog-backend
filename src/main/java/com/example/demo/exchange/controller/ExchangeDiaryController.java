package com.example.demo.exchange.controller;

import com.example.demo.exchange.dto.ExchangeDiaryRequestDto;
import com.example.demo.exchange.dto.ExchangeDiaryResponseDto;
import com.example.demo.exchange.service.ExchangeDiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exchange/diaries")
@RequiredArgsConstructor
public class ExchangeDiaryController {

    private final ExchangeDiaryService diaryService;

    // 일기 작성
    @PostMapping
    public ResponseEntity<ExchangeDiaryResponseDto> createDiary(@RequestBody ExchangeDiaryRequestDto request) {
        return ResponseEntity.ok(diaryService.createDiary(request));
    }

    // 일기 수정
    @PatchMapping("/{diaryId}")
    public ResponseEntity<ExchangeDiaryResponseDto> updateDiary(
            @PathVariable Long diaryId,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(diaryService.updateDiary(diaryId, body.get("content")));
    }

    // 세션별 일기 목록 조회
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<ExchangeDiaryResponseDto>> getDiaries(@PathVariable Long sessionId) {
        return ResponseEntity.ok(diaryService.getDiariesBySession(sessionId));
    }
}