package com.plog.api.pipeline.dto;

/**
 * [Day 8.10] 다중 이미지 Gemini 전달용. Gemini multi-turn에서 첫 user 메시지에
 * 모든 사진의 inline_data + 캡션(시간/위치/장면)을 함께 전송하기 위한 묶음.
 *
 * @param bytes    이미지 원본 byte (압축 전)
 * @param mimeType "image/jpeg" 등
 * @param caption  "사진1 (09:12, 제주 한라산 인근, 벚꽃 산책)" 같은 1줄 요약. Gemini가
 *                 사진 간 흐름·인과를 사고하는 단서로 사용. null/blank 허용.
 */
public record ImagePart(byte[] bytes, String mimeType, String caption) {}
