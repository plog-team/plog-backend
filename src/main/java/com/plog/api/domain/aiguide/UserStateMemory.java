package com.plog.api.domain.aiguide;

import com.plog.api.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_state_memory",
    uniqueConstraints = @UniqueConstraint(name = "uk_usm_user_key", columnNames = {"user_id", "memory_key"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStateMemory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "memory_key", nullable = false, length = 100)
    private String memoryKey;

    @Lob
    @Column(name = "value_json", nullable = false, columnDefinition = "LONGTEXT")
    private String valueJson;

    @Builder
    private UserStateMemory(Long userId, String memoryKey, String valueJson) {
        this.userId = userId;
        this.memoryKey = memoryKey;
        this.valueJson = valueJson;
    }

    public void updateValue(String valueJson) {
        this.valueJson = valueJson;
    }
}
