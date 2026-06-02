package com.plog.api.domain.recommend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserLocationLogRepository extends JpaRepository<UserLocationLog, Long> {
    Optional<UserLocationLog> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
