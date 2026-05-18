package com.plog.api.pipeline;

import org.springframework.stereotype.Component;

import com.plog.api.domain.aiguide.Persona;
import com.plog.api.llm.GeminiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 페르소나 + 사용자 가이드 MD + context(사진 vision + 답변/대화)를
 * Gemini로 일기 초안 생성.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiDraftNode {

    private final GeminiClient geminiClient;

    public String generate(Persona persona, String userDiaryGuideMd, String contextDescription) {
        Persona p = Persona.orDefault(persona);
        long t0 = System.currentTimeMillis();
        String draft = geminiClient.generateDraft(p.getSystemPromptFragment(), userDiaryGuideMd, contextDescription);
        log.info("GeminiDraftNode persona={} guide_len={} ctx_len={} latency={}ms draft_chars={}",
                p.name(),
                userDiaryGuideMd == null ? 0 : userDiaryGuideMd.length(),
                contextDescription == null ? 0 : contextDescription.length(),
                System.currentTimeMillis() - t0,
                draft.length());
        return draft;
    }
}
