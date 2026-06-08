package com.plog.api.exchange.repository;

import com.plog.api.exchange.domain.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    List<Block> findByBlockerId(Long blockerId);
    List<Block> findByBlockedId(Long blockedId);
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}