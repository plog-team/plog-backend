package com.plog.api.domain.aiguide;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.llm.GeminiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자별 일기 작성 취향 가이드(MD, 20줄 max).
 * 피드백 코멘트가 들어올 때마다 기존 MD에 자연 누적 → Gemini로 재정리.
 * 저장 위치: user_state_memory (key=DIARY_GUIDE_KEY).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryGuideService {

    public static final String DIARY_GUIDE_KEY = "diary_guide_md";
    public static final int MAX_LINES = 20;

    private final StateMemoryService stateMemoryService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<String> getGuide(long userId) {
        return stateMemoryService.getRawJson(userId, DIARY_GUIDE_KEY)
                .map(this::unwrapJsonString);
    }

    @Transactional
    public String saveGuide(long userId, String md) {
        String trimmed = trimToMaxLines(md == null ? "" : md.trim(), MAX_LINES);
        stateMemoryService.upsert(userId, DIARY_GUIDE_KEY, trimmed);
        return trimmed;
    }

    /**
     * 사용자 피드백 코멘트를 기존 가이드 MD에 자연 누적.
     * Gemini로 새 피드백 + 기존 MD를 종합해 20줄 이내로 압축·재정리.
     * 실패 시 기존 MD 유지(피드백 API의 핵심 흐름 방해 금지).
     */
    @Transactional
    public void appendFromFeedback(long userId, int satisfactionScore, String comment) {
        if (comment == null || comment.isBlank()) {
            return;
        }
        String existing = getGuide(userId).orElse("");
        try {
            String newMd = geminiClient.refineDiaryGuide(existing, comment, satisfactionScore, MAX_LINES);
            String trimmed = trimToMaxLines(newMd, MAX_LINES);
            stateMemoryService.upsert(userId, DIARY_GUIDE_KEY, trimmed);
            log.info("DiaryGuide updated userId={} lines={}", userId, countLines(trimmed));
        } catch (Exception e) {
            log.warn("DiaryGuide 갱신 실패 (기존 유지) userId={} err={}", userId, e.getMessage());
        }
    }

    /** state_memory는 JSON 직렬화 저장이므로 따옴표로 감싼 String 형태일 수 있다. unwrap. */
    private String unwrapJsonString(String raw) {
        if (raw == null) return null;
        try {
            return objectMapper.readValue(raw, String.class);
        } catch (Exception e) {
            return raw;
        }
    }

    private String trimToMaxLines(String md, int max) {
        if (md == null) return "";
        List<String> lines = List.of(md.split("\\r?\\n"));
        if (lines.size() <= max) return md;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(lines.get(i));
            if (i < max - 1) sb.append('\n');
        }
        return sb.toString();
    }

    private int countLines(String md) {
        return md == null || md.isEmpty() ? 0 : md.split("\\r?\\n").length;
    }
}
