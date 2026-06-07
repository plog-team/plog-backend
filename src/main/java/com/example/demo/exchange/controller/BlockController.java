package com.example.demo.exchange.controller;

import com.example.demo.exchange.dto.BlockRequestDto;
import com.example.demo.exchange.dto.BlockResponseDto;
import com.example.demo.exchange.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange/blocks")
public class BlockController {

    private final BlockService blockService;

    // 차단
    @PostMapping
    public ResponseEntity<BlockResponseDto> createBlock(@RequestBody BlockRequestDto request) {
        return ResponseEntity.ok(blockService.createBlock(request));
    }

    // 차단 해제
    @DeleteMapping
    public ResponseEntity<Void> deleteBlock(
            @RequestParam Long blockerId,
            @RequestParam Long blockedId) {
        blockService.deleteBlock(blockerId, blockedId);
        return ResponseEntity.ok().build();
    }

    // 차단 목록 조회
    @GetMapping
    public ResponseEntity<List<BlockResponseDto>> getBlockList(@RequestParam Long blockerId) {
        return ResponseEntity.ok(blockService.getBlockList(blockerId));
    }
}