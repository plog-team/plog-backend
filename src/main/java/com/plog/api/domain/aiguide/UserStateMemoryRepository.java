package com.plog.api.domain.aiguide;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStateMemoryRepository extends JpaRepository<UserStateMemory, Long> {
    Optional<UserStateMemory> findByUserIdAndMemoryKey(Long userId, String memoryKey);
    List<UserStateMemory> findAllByUserId(Long userId);
}
