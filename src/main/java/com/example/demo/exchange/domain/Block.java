package com.example.demo.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "user_block")
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blocker_id")
    private Long blockerId;

    @Column(name = "blocked_user_id")
    private Long blockedId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Block(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = LocalDateTime.now();
    }
}