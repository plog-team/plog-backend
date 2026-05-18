package com.plog.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * [Day 8.7] CORS 설정 (명세 §15-1 + §10 Risk #4).
 *
 * 안드로이드 에뮬레이터(10.0.2.2:8080)는 same-origin이라 영향 없으나,
 * 다른 팀원이 웹/외부 도구에서 호출 시 차단되지 않도록 개방.
 * 배포 시에는 origins를 실제 도메인으로 제한 필요.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-User-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
