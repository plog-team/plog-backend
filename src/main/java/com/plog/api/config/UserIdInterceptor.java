package com.plog.api.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.plog.api.common.UserContext;
import com.plog.api.common.exception.BadRequestException;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserIdInterceptor implements HandlerInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BadRequestException("Authorization 헤더가 필요합니다");
        }
        String token = header.substring(7);
        try {
            long userId = jwtProvider.extractUserId(token);
            UserContext.set(userId);
            return true;
        } catch (JwtException e) {
            throw new BadRequestException("유효하지 않은 토큰입니다");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
