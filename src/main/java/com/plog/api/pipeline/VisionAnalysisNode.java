package com.plog.api.pipeline;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.common.exception.BadRequestException;
import com.plog.api.domain.cache.ImageAnalysisCache;
import com.plog.api.domain.cache.ImageAnalysisCacheRepository;
import com.plog.api.llm.GeminiClient;
import com.plog.api.pipeline.dto.VisionResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class VisionAnalysisNode {

    private final GeminiClient geminiClient;
    private final ImageAnalysisCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    @Value("${plog.gemini.model:gemini-1.5-flash}")
    private String modelVersion;

    public record Output(VisionResult vision, boolean cacheHit, long latencyMs) {}

    @Transactional
    public Output analyze(byte[] imageBytes, String sha256, String mimeType) {
        long t0 = System.currentTimeMillis();
        Optional<ImageAnalysisCache> hit = cacheRepository.findBySha256(sha256);
        if (hit.isPresent()) {
            try {
                VisionResult cached = objectMapper.readValue(hit.get().getVisionJson(), VisionResult.class);
                long elapsed = System.currentTimeMillis() - t0;
                log.info("Vision CACHE HIT sha={} {}ms", sha256, elapsed);
                return new Output(cached, true, elapsed);
            } catch (Exception e) {
                log.warn("캐시 JSON 파싱 실패, 재호출. sha={} err={}", sha256, e.getMessage());
            }
        }

        VisionResult vision = geminiClient.analyzeImage(imageBytes, mimeType);
        try {
            String json = objectMapper.writeValueAsString(vision);
            cacheRepository.save(ImageAnalysisCache.builder()
                    .sha256(sha256)
                    .visionJson(json)
                    .modelVersion(modelVersion)
                    .build());
        } catch (Exception e) {
            throw new BadRequestException("Vision 결과 직렬화 실패: " + e.getMessage());
        }
        long elapsed = System.currentTimeMillis() - t0;
        log.info("Vision CACHE MISS sha={} {}ms model={}", sha256, elapsed, modelVersion);
        return new Output(vision, false, elapsed);
    }
}
