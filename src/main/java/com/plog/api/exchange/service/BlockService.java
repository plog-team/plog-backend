package com.plog.api.exchange.service;

import com.plog.api.exchange.domain.Block;
import com.plog.api.exchange.dto.BlockResponseDto;
import com.plog.api.exchange.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;

    // 차단
    @Transactional
    public BlockResponseDto createBlock(Long blockerId, Long blockedId) {
        Block block = new Block(blockerId, blockedId);
        blockRepository.save(block);
        return new BlockResponseDto(block);
    }

    // 차단 해제
    @Transactional
    public void deleteBlock(Long blockerId, Long blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new RuntimeException("차단 정보를 찾을 수 없습니다."));
        blockRepository.delete(block);
    }

    // 차단 목록 조회
    @Transactional(readOnly = true)
    public List<BlockResponseDto> getBlockList(Long blockerId) {
        return blockRepository.findByBlockerId(blockerId)
                .stream()
                .map(BlockResponseDto::new)
                .collect(Collectors.toList());
    }
}