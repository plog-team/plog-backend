package com.example.demo.exchange.repository;

import com.example.demo.exchange.domain.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    List<Block> findByBlockerId(Long blockerId);
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}