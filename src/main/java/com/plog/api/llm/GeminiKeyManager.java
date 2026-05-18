package com.plog.api.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Gemini API 키 자동 회전 매니저.
 * 키당 free tier quota 한도에 도달하면 다음 키로 자동 전환.
 *
 * 설정: application-local.yml의 plog.gemini.api-keys 리스트.
 * 단일 키만 설정된 경우 plog.gemini.api-key로 fallback.
 */
@Slf4j
@Component
public class GeminiKeyManager {

    private final List<String> keys;
    private final AtomicInteger idx = new AtomicInteger(0);

    public GeminiKeyManager(
            @Value("${plog.gemini.api-keys:}") String rawKeysCsv,
            @Value("${plog.gemini.api-key:}") String singleKey) {
        List<String> result = new ArrayList<>();
        if (rawKeysCsv != null && !rawKeysCsv.isBlank()) {
            for (String k : rawKeysCsv.split(",")) {
                String t = k.trim();
                if (!t.isEmpty()) result.add(t);
            }
        }
        if (result.isEmpty() && singleKey != null && !singleKey.isBlank()) {
            result.add(singleKey.trim());
        }
        this.keys = List.copyOf(result);
        log.info("GeminiKeyManager 초기화: 키 {} 개 로드됨", this.keys.size());
    }

    public String current() {
        if (keys.isEmpty()) {
            throw new IllegalStateException(
                "Gemini api-keys 설정 없음. application-local.yml의 plog.gemini.api-keys 확인.");
        }
        return keys.get(idx.get() % keys.size());
    }

    public int currentIndex() {
        if (keys.isEmpty()) return -1;
        return idx.get() % keys.size();
    }

    public int size() {
        return keys.size();
    }

    public void rotate() {
        int next = idx.incrementAndGet();
        log.info("Gemini 키 회전: idx={} → idx={} (size={})",
                (next - 1) % Math.max(1, keys.size()),
                next % Math.max(1, keys.size()),
                keys.size());
    }
}
