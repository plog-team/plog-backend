package com.plog.api.exchange.controller;

import com.plog.api.common.UserContext;
import com.plog.api.exchange.dto.ExchangeMatchRequestDto;
import com.plog.api.exchange.dto.ExchangeMatchResponseDto;
import com.plog.api.exchange.dto.ExchangeRoomResponseDto;
import com.plog.api.exchange.service.ExchangeMatchService;
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
        return ResponseEntity.ok(matchService.createMatch(UserContext.get(), request.getTargetUserId()));
    }

    @PostMapping("/{matchId}/accept")
    public ResponseEntity<ExchangeRoomResponseDto> acceptMatch(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.acceptMatch(matchId));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<ExchangeMatchResponseDto> getMatch(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.getMatch(matchId, UserContext.get()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ExchangeMatchResponseDto>> getPendingMatches() {
        return ResponseEntity.ok(matchService.getPendingMatches(UserContext.get()));
    }

    @PostMapping("/{matchId}/reject")
    public ResponseEntity<Void> rejectMatch(@PathVariable Long matchId) {
        matchService.rejectMatch(matchId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-active")
    public ResponseEntity<ExchangeMatchResponseDto> getMyActiveMatch() {
        ExchangeMatchResponseDto match = matchService.getMyPendingMatch(UserContext.get());
        if (match == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(match);
    }
}