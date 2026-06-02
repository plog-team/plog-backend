package com.example.demo.exchange.controller;

import com.example.demo.exchange.dto.MatchRecommendResponseDto;
import com.example.demo.exchange.service.MatchRecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/exchange/recommend")
@RequiredArgsConstructor
public class MatchRecommendController {

    private final MatchRecommendService matchRecommendService;

    @GetMapping
    public ResponseEntity<List<MatchRecommendResponseDto>> recommendMatches(@RequestParam Long userId) {
        return ResponseEntity.ok(matchRecommendService.recommendMatches(userId));
    }
}