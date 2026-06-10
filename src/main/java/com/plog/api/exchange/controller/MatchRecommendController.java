package com.plog.api.exchange.controller;

import com.plog.api.common.UserContext;
import com.plog.api.exchange.dto.MatchRecommendResponseDto;
import com.plog.api.exchange.service.MatchRecommendService;
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
    public ResponseEntity<List<MatchRecommendResponseDto>> recommendMatches() {
        return ResponseEntity.ok(matchRecommendService.recommendMatches(UserContext.get()));
    }
}