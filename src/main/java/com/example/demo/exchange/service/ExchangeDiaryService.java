package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.ExchangeDiary;
import com.example.demo.exchange.domain.ExchangeSession;
import com.example.demo.exchange.dto.ExchangeDiaryRequestDto;
import com.example.demo.exchange.dto.ExchangeDiaryResponseDto;
import com.example.demo.exchange.repository.ExchangeDiaryRepository;
import com.example.demo.exchange.repository.ExchangeSessionRepository;
import com.example.demo.exchange.repository.SessionParticipantRepository;
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

    // 일기 작성
    @Transactional
    public ExchangeDiaryResponseDto createDiary(ExchangeDiaryRequestDto request) {
        ExchangeSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));

        ExchangeDiary diary = new ExchangeDiary(
                session,
                request.getUserId(),
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

    // 일기 수정
    @Transactional
    public ExchangeDiaryResponseDto updateDiary(Long diaryId, String content) {
        ExchangeDiary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("일기를 찾을 수 없습니다."));
        diary.updateContent(content);
        return new ExchangeDiaryResponseDto(diary);
    }

    // 세션의 일기 목록 조회
    @Transactional(readOnly = true)
    public List<ExchangeDiaryResponseDto> getDiariesBySession(Long sessionId) {
        ExchangeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));

        return diaryRepository.findAll().stream()
                .filter(d -> d.getExchangeSession().getId().equals(sessionId))
                .map(ExchangeDiaryResponseDto::new)
                .collect(Collectors.toList());
    }
}