package com.plog.api.exchange.service;

import com.plog.api.exchange.domain.Notification;
import com.plog.api.exchange.dto.NotificationResponseDto;
import com.plog.api.exchange.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseDto::new)
                .collect(Collectors.toList());
    }

    // 읽지 않은 알림 조회
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseDto::new)
                .collect(Collectors.toList());
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));
        notification.markAsRead();
    }

    // 알림 생성
    @Transactional
    public void createNotification(Long userId, String type, Long targetId, String targetType) {
        Notification notification = new Notification(userId, type, targetId, targetType);
        notificationRepository.save(notification);
    }
}