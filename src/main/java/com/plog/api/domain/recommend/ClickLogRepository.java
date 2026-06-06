package com.plog.api.domain.recommend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {

    // 유저별 카테고리 클릭 수 집계 (많이 클릭한 순)
    @Query("SELECT c.contentTypeId, COUNT(c) as cnt " +
            "FROM ClickLog c " +
            "WHERE c.userId = :userId AND c.contentTypeId IS NOT NULL " +
            "GROUP BY c.contentTypeId " +
            "ORDER BY cnt DESC")
    List<Object[]> findTopCategoriesByUserId(@Param("userId") Long userId);
}
