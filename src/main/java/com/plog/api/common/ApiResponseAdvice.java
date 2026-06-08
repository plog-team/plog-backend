package com.plog.api.common;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * [Day 8.6 C] 모든 컨트롤러 응답을 ApiResponse<T>로 자동 wrap.
 *
 * - 정상 응답: { "success": true, "data": <원본 객체>, "error": null }
 * - 예외: GlobalExceptionHandler가 ApiResponse.fail로 직접 빌드
 *
 * 예외:
 * - 응답이 이미 ApiResponse면 wrap 스킵 (중복 방지)
 * - String 응답은 wrap 시 ContentType 충돌 위험 → 그대로 둠
 */
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> param = returnType.getParameterType();
        if (ApiResponse.class.isAssignableFrom(param)) return false;
        if (String.class.equals(param)) return false;
        // 에러 핸들러(@ExceptionHandler)의 ResponseEntity<Map> 응답은 그대로 둠
        if (returnType.hasMethodAnnotation(ExceptionHandler.class)) return false;
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 이미지/파일 byte[] 응답은 ApiResponse로 감싸면 안 됨
        if (body instanceof byte[]) return body;

        // 이미 ApiResponse면 그대로
        if (body instanceof ApiResponse<?>) return body;

        return ApiResponse.ok(body);
    }
}
