package com.plog.api.exchange.controller;

import com.plog.api.common.UserContext;
import com.plog.api.exchange.dto.BlockRequestDto;
import com.plog.api.exchange.dto.BlockResponseDto;
import com.plog.api.exchange.service.BlockService;
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
        return ResponseEntity.ok(blockService.createBlock(UserContext.get(), request.getBlockedId()));
    }

    // 차단 해제
    @DeleteMapping
    public ResponseEntity<Void> deleteBlock(@RequestParam Long blockedId) {
        blockService.deleteBlock(UserContext.get(), blockedId);
        return ResponseEntity.ok().build();
    }

    // 차단 목록 조회
    @GetMapping
    public ResponseEntity<List<BlockResponseDto>> getBlockList() {
        return ResponseEntity.ok(blockService.getBlockList(UserContext.get()));
    }
}