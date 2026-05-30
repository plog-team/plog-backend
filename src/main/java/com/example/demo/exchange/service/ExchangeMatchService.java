package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.ExchangeMatch;
import com.example.demo.exchange.domain.ExchangeRoom;
import com.example.demo.exchange.domain.MatchParticipant;
import com.example.demo.exchange.dto.ExchangeMatchResponseDto;
import com.example.demo.exchange.dto.ExchangeRoomResponseDto;
import com.example.demo.exchange.repository.ExchangeMatchRepository;
import com.example.demo.exchange.repository.ExchangeRoomRepository;
import com.example.demo.exchange.repository.MatchParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExchangeMatchService {

    private final ExchangeMatchRepository matchRepository;
    private final MatchParticipantRepository participantRepository;
    private final ExchangeRoomRepository roomRepository;

    // 매칭 신청
    @Transactional
    public ExchangeMatchResponseDto createMatch(Long userId) {
        ExchangeMatch match = new ExchangeMatch("PENDING", LocalDateTime.now());
        matchRepository.save(match);

        MatchParticipant participant = new MatchParticipant(match, userId);
        participantRepository.save(participant);

        return new ExchangeMatchResponseDto(match);
    }

    // 매칭 수락 (방 생성)
    @Transactional
    public ExchangeRoomResponseDto acceptMatch(Long matchId) {
        ExchangeMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("매칭을 찾을 수 없습니다."));

        match.updateStatus("MATCHED");

        ExchangeRoom room = new ExchangeRoom(match, "ACTIVE", LocalDateTime.now());
        roomRepository.save(room);

        return new ExchangeRoomResponseDto(room);
    }

    // 매칭 조회
    @Transactional(readOnly = true)
    public ExchangeMatchResponseDto getMatch(Long matchId) {
        ExchangeMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("매칭을 찾을 수 없습니다."));
        return new ExchangeMatchResponseDto(match);
    }
}