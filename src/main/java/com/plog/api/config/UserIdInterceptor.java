package com.plog.api.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.plog.api.common.UserContext;
import com.plog.api.common.exception.BadRequestException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserIdInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-User-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String raw = request.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("X-User-Id 헤더가 필요합니다");
        }
        long userId;
        try {
            userId = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("X-User-Id 헤더는 정수여야 합니다 (got: " + raw + ")");
        }
        if (userId <= 0) {
            throw new BadRequestException("X-User-Id 헤더는 1 이상이어야 합니다");
        }
        UserContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
