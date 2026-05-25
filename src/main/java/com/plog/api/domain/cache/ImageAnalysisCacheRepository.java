package com.plog.api.domain.cache;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageAnalysisCacheRepository extends JpaRepository<ImageAnalysisCache, Long> {
    Optional<ImageAnalysisCache> findBySha256(String sha256);
}
