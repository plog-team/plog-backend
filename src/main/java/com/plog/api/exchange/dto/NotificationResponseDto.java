package com.plog.api.exchange.dto;

import com.plog.api.exchange.domain.Notification;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class NotificationResponseDto {
    private Long id;
    private String type;
    private Long targetId;
    private String targetType;
    private LocalDateTime createdAt;
    private boolean isRead;
    private String message;

    public NotificationResponseDto(Notification notification) {
        this.id = notification.getId();
        this.type = notification.getType();
        this.targetId = notification.getTargetId();
        this.targetType = notification.getTargetType();
        this.createdAt = notification.getCreatedAt();
        this.isRead = notification.isRead();
        this.message = generateMessage(notification.getType());
    }

    private String generateMessage(String type) {
        switch (type) {
            case "MATCH_REQUEST": return "새로운 매칭 신청이 왔어요!";
            case "MATCH_ACCEPTED": return "매칭이 수락됐어요!";
            case "DIARY_WRITTEN": return "상대방이 일기를 작성했어요!";
            case "SESSION_EXPIRING": return "교환일기 기간이 곧 만료돼요!";
            default: return "새로운 알림이 있어요.";
        }
    }
}