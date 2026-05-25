package com.plog.api.common;

import lombok.Builder;

/**
 * 공통 응답 래퍼 {success, data, error}.
 * ApiResponseAdvice가 모든 컨트롤러 반환값을 자동으로 이 형식으로 감쌈.
 */
@Builder
public record ApiResponse<T>(
    boolean success,
    T data,
    String error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).error(null).build();
    }

    public static <T> ApiResponse<T> fail(String error) {
        return ApiResponse.<T>builder().success(false).data(null).error(error).build();
    }
}
