package com.plog.api.exchange.service;

import com.plog.api.exchange.domain.ExchangeDiary;
import com.plog.api.exchange.domain.ExchangeSession;
import com.plog.api.exchange.dto.ExchangeDiaryRequestDto;
import com.plog.api.exchange.dto.ExchangeDiaryResponseDto;
import com.plog.api.exchange.repository.ExchangeDiaryRepository;
import com.plog.api.exchange.repository.ExchangeSessionRepository;
import com.plog.api.exchange.repository.SessionParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExchangeDiaryService {

    private final ExchangeDiaryRepository diaryRepository;
    private final ExchangeSessionRepository sessionRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final NotificationService notificationService;

    @Transactional
    public ExchangeDiaryResponseDto createDiary(ExchangeDiaryRequestDto request) {
        ExchangeSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));

        ExchangeDiary diary = new ExchangeDiary(
                session,
                request.getUserId(),
                request.getTitle(),
                request.getContent(),
                request.getDayNumber(),
                LocalDateTime.now()
        );

        ExchangeDiary saved = diaryRepository.save(diary);

        sessionParticipantRepository.findByExchangeSessionId(session.getId())
                .stream()
                .filter(p -> !p.getUserId().equals(request.getUserId()))
                .findFirst()
                .ifPresent(p -> notificationService.createNotification(
                        p.getUserId(), "DIARY_WRITTEN", saved.getId(), "DIARY"));

        return new ExchangeDiaryResponseDto(saved);
    }

    @Transactional
    public ExchangeDiaryResponseDto updateDiary(Long diaryId, String title, String content) {
        ExchangeDiary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("일기를 찾을 수 없습니다."));
        diary.updateContent(title, content);
        return new ExchangeDiaryResponseDto(diary);
    }

    @Transactional(readOnly = true)
    public List<ExchangeDiaryResponseDto> getDiariesBySession(Long sessionId) {
        ExchangeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));

        return diaryRepository.findAll().stream()
                .filter(d -> d.getExchangeSession().getId().equals(sessionId))
                .map(ExchangeDiaryResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExchangeDiaryResponseDto getDiary(Long diaryId) {
        ExchangeDiary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("일기를 찾을 수 없습니다."));
        return new ExchangeDiaryResponseDto(diary);
    }
}