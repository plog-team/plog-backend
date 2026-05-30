package com.example.demo.exchange.controller;

import com.example.demo.exchange.dto.ExchangeMatchRequestDto;
import com.example.demo.exchange.dto.ExchangeMatchResponseDto;
import com.example.demo.exchange.dto.ExchangeRoomResponseDto;
import com.example.demo.exchange.service.ExchangeMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/exchange/matches")
@RequiredArgsConstructor
public class ExchangeMatchController {

    private final ExchangeMatchService matchService;

    // 매칭 신청
    @PostMapping
    public ResponseEntity<ExchangeMatchResponseDto> createMatch(@RequestBody ExchangeMatchRequestDto request) {
        return ResponseEntity.ok(matchService.createMatch(request.getUserId()));
    }

    // 매칭 수락 (방 생성)
    @PostMapping("/{matchId}/accept")
    public ResponseEntity<ExchangeRoomResponseDto> acceptMatch(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.acceptMatch(matchId));
    }

    // 매칭 조회
    @GetMapping("/{matchId}")
    public ResponseEntity<ExchangeMatchResponseDto> getMatch(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.getMatch(matchId));
    }

    // 대기 중인 매칭 목록 조회
    @GetMapping("/pending")
    public ResponseEntity<List<ExchangeMatchResponseDto>> getPendingMatches() {
        return ResponseEntity.ok(matchService.getPendingMatches());
    }

    // 매칭 거절
    @PostMapping("/{matchId}/reject")
    public ResponseEntity<Void> rejectMatch(@PathVariable Long matchId) {
        matchService.rejectMatch(matchId);
        return ResponseEntity.ok().build();
    }
}