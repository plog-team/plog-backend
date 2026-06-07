package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.ExchangeRoom;
import com.example.demo.exchange.domain.ExchangeSession;
import com.example.demo.exchange.domain.MatchParticipant;
import com.example.demo.exchange.domain.SessionParticipant;
import com.example.demo.exchange.dto.ExchangeSessionResponseDto;
import com.example.demo.exchange.repository.ExchangeRoomRepository;
import com.example.demo.exchange.repository.ExchangeSessionRepository;
import com.example.demo.exchange.repository.MatchParticipantRepository;
import com.example.demo.exchange.repository.SessionParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeSessionService {

    private final ExchangeSessionRepository sessionRepository;
    private final ExchangeRoomRepository roomRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    // 세션 시작
    @Transactional
    public ExchangeSessionResponseDto startSession(Long roomId) {
        ExchangeRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("교환방을 찾을 수 없습니다."));

        return sessionRepository.findByExchangeRoomId(roomId)
                .map(ExchangeSessionResponseDto::new)
                .orElseGet(() -> {
                    ExchangeSession session = new ExchangeSession(room, LocalDate.now(), "ACTIVE");
                    sessionRepository.save(session);

                    // 매칭 참가자를 세션 참가자로 등록
                    Long matchId = room.getExchangeMatch().getId();
                    List<MatchParticipant> matchParticipants = matchParticipantRepository.findAllByExchangeMatchId(matchId);
                    for (MatchParticipant mp : matchParticipants) {
                        SessionParticipant sp = new SessionParticipant(session, mp.getUserId());
                        sessionParticipantRepository.save(sp);
                    }

                    return new ExchangeSessionResponseDto(session);
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

    // 세션 연장 동의
    @Transactional
    public ExchangeSessionResponseDto agreeExtend(Long sessionId, Long userId) {
        ExchangeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));

        SessionParticipant participant = sessionParticipantRepository
                .findByExchangeSessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("참가자를 찾을 수 없습니다."));
        participant.agreeExtend();

        List<SessionParticipant> participants = sessionParticipantRepository.findByExchangeSessionId(sessionId);
        boolean allAgreed = participants.stream().allMatch(SessionParticipant::isExtendAgreed);

        if (allAgreed) {
            session.extend();
            participants.forEach(p -> p.agreeExtend());
        }

        return new ExchangeSessionResponseDto(session);
    }

    // 연장 동의 여부 조회
    @Transactional(readOnly = true)
    public Map<String, Boolean> getExtendStatus(Long sessionId) {
        List<SessionParticipant> participants = sessionParticipantRepository.findByExchangeSessionId(sessionId);
        boolean allAgreed = participants.stream().allMatch(SessionParticipant::isExtendAgreed);
        boolean anyAgreed = participants.stream().anyMatch(SessionParticipant::isExtendAgreed);
        Map<String, Boolean> result = new HashMap<>();
        result.put("allAgreed", allAgreed);
        result.put("anyAgreed", anyAgreed);
        return result;
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