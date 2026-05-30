package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.AppUser;
import com.example.demo.exchange.domain.ExchangeMatch;
import com.example.demo.exchange.domain.ExchangeRoom;
import com.example.demo.exchange.domain.MatchParticipant;
import com.example.demo.exchange.dto.ExchangeMatchResponseDto;
import com.example.demo.exchange.dto.ExchangeRoomResponseDto;
import com.example.demo.exchange.repository.AppUserRepository;
import com.example.demo.exchange.repository.ExchangeMatchRepository;
import com.example.demo.exchange.repository.ExchangeRoomRepository;
import com.example.demo.exchange.repository.MatchParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeMatchService {

    private final ExchangeMatchRepository matchRepository;
    private final MatchParticipantRepository participantRepository;
    private final ExchangeRoomRepository roomRepository;
    private final AppUserRepository appUserRepository;

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

    // 대기 중인 매칭 목록 조회
    @Transactional(readOnly = true)
    public List<ExchangeMatchResponseDto> getPendingMatches() {
        return matchRepository.findByStatus("PENDING")
                .stream()
                .map(match -> {
                    String nickname = participantRepository.findByExchangeMatchId(match.getId())
                            .flatMap(p -> appUserRepository.findById(p.getUserId()))
                            .map(AppUser::getNickname)
                            .orElse("사용자");
                    return new ExchangeMatchResponseDto(match, nickname);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // 매칭 거절
    @Transactional
    public void rejectMatch(Long matchId) {
        ExchangeMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("매칭을 찾을 수 없습니다."));
        match.updateStatus("REJECTED");
    }
}