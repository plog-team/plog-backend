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

    @PostMapping
    public ResponseEntity<ExchangeMatchResponseDto> createMatch(@RequestBody ExchangeMatchRequestDto request) {
        return ResponseEntity.ok(matchService.createMatch(request.getUserId(), request.getTargetUserId()));
    }

    @PostMapping("/{matchId}/accept")
    public ResponseEntity<ExchangeRoomResponseDto> acceptMatch(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.acceptMatch(matchId));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<ExchangeMatchResponseDto> getMatch(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.getMatch(matchId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ExchangeMatchResponseDto>> getPendingMatches() {
        return ResponseEntity.ok(matchService.getPendingMatches());
    }

    @PostMapping("/{matchId}/reject")
    public ResponseEntity<Void> rejectMatch(@PathVariable Long matchId) {
        matchService.rejectMatch(matchId);
        return ResponseEntity.ok().build();
    }

    // 내 활성 매칭 조회
    @GetMapping("/my-active")
    public ResponseEntity<ExchangeMatchResponseDto> getMyActiveMatch(@RequestParam Long userId) {
        ExchangeMatchResponseDto match = matchService.getMyPendingMatch(userId);
        if (match == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(match);
    }
}