package com.plog.api.common.exception;

/**
 * [Day 8.6 E] Gemini quota 초과(429) 등 일시적 차단 상황 표시용.
 * GlobalExceptionHandler가 429 응답으로 매핑.
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
