package com.example.demo.exchange.controller;

import com.example.demo.exchange.dto.ExchangeSessionResponseDto;
import com.example.demo.exchange.service.ExchangeSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchange/sessions")
@RequiredArgsConstructor
public class ExchangeSessionController {

    private final ExchangeSessionService sessionService;

    // 세션 시작
    @PostMapping("/{roomId}")
    public ResponseEntity<ExchangeSessionResponseDto> startSession(@PathVariable Long roomId) {
        return ResponseEntity.ok(sessionService.startSession(roomId));
    }

    // 세션 조회
    @GetMapping("/{sessionId}")
    public ResponseEntity<ExchangeSessionResponseDto> getSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    // 세션 종료
    @PatchMapping("/{sessionId}/end")
    public ResponseEntity<ExchangeSessionResponseDto> endSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.endSession(sessionId));
    }
}