package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.ExchangeRoom;
import com.example.demo.exchange.domain.ExchangeSession;
import com.example.demo.exchange.dto.ExchangeSessionResponseDto;
import com.example.demo.exchange.repository.ExchangeRoomRepository;
import com.example.demo.exchange.repository.ExchangeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExchangeSessionService {

    private final ExchangeSessionRepository sessionRepository;
    private final ExchangeRoomRepository roomRepository;

    // 세션 시작
    @Transactional
    public ExchangeSessionResponseDto startSession(Long roomId) {
        ExchangeRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("교환방을 찾을 수 없습니다."));

        // 이미 세션이 있으면 기존 세션 반환
        return sessionRepository.findByExchangeRoomId(roomId)
                .map(ExchangeSessionResponseDto::new)
                .orElseGet(() -> {
                    ExchangeSession session = new ExchangeSession(room, LocalDate.now(), "ACTIVE");
                    return new ExchangeSessionResponseDto(sessionRepository.save(session));
                });
    }

    // sessionId로 세션 조회
    @Transactional(readOnly = true)
    public ExchangeSessionResponseDto getSession(Long sessionId) {
        ExchangeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));
        return new ExchangeSessionResponseDto(session);
    }

    // roomId로 세션 조회
    @Transactional(readOnly = true)
    public ExchangeSessionResponseDto getSessionByRoomId(Long roomId) {
        ExchangeSession session = sessionRepository.findByExchangeRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));
        return new ExchangeSessionResponseDto(session);
    }

    // 세션 종료
    @Transactional
    public ExchangeSessionResponseDto endSession(Long sessionId) {
        ExchangeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));
        session.end(LocalDate.now());
        return new ExchangeSessionResponseDto(session);
    }
}