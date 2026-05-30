package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.ExchangeDiary;
import com.example.demo.exchange.domain.ExchangeSession;
import com.example.demo.exchange.dto.ExchangeDiaryRequestDto;
import com.example.demo.exchange.dto.ExchangeDiaryResponseDto;
import com.example.demo.exchange.repository.ExchangeDiaryRepository;
import com.example.demo.exchange.repository.ExchangeSessionRepository;
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

        return new ExchangeDiaryResponseDto(diaryRepository.save(diary));
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