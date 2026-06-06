package com.example.demo.exchange.controller;

import com.example.demo.exchange.dto.ExchangeSessionResponseDto;
import com.example.demo.exchange.service.ExchangeSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange/sessions")
@RequiredArgsConstructor
public class ExchangeSessionController {

    private final ExchangeSessionService sessionService;

    // 세션 시작 (없으면 생성, 있으면 기존 반환)
    @PostMapping("/{roomId}")
    public ResponseEntity<ExchangeSessionResponseDto> startSession(@PathVariable Long roomId) {
        return ResponseEntity.ok(sessionService.startSession(roomId));
    }

    // sessionId로 세션 조회
    @GetMapping("/{sessionId}")
    public ResponseEntity<ExchangeSessionResponseDto> getSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    // roomId로 세션 조회
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ExchangeSessionResponseDto> getSessionByRoomId(@PathVariable Long roomId) {
        return ResponseEntity.ok(sessionService.getSessionByRoomId(roomId));
    }

    // 세션 연장 동의
    @PostMapping("/{sessionId}/extend")
    public ResponseEntity<ExchangeSessionResponseDto> agreeExtend(
            @PathVariable Long sessionId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(sessionService.agreeExtend(sessionId, userId));
    }

    // 세션 종료
    @PatchMapping("/{sessionId}/end")
    public ResponseEntity<ExchangeSessionResponseDto> endSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.endSession(sessionId));
    }

    // 연장 동의 여부 조회
    @GetMapping("/{sessionId}/extend-status")
    public ResponseEntity<Map<String, Boolean>> getExtendStatus(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getExtendStatus(sessionId));
    }
}