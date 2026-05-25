package com.plog.api.pipeline.dto;

import java.util.List;

/**
 * [Day 8.11] BATCH 모드 — Gemini 질문 N개 응답 묶음.
 */
public record BatchQuestionsResponse(List<BatchQuestion> questions) {}
