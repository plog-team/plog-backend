package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.AppUser;
import com.example.demo.exchange.domain.ExchangeMatch;
import com.example.demo.exchange.domain.ExchangeRoom;
import com.example.demo.exchange.domain.MatchParticipant;
import com.example.demo.exchange.domain.UserPreferenceScore;
import com.example.demo.exchange.dto.ExchangeMatchResponseDto;
import com.example.demo.exchange.dto.ExchangeRoomResponseDto;
import com.example.demo.exchange.repository.AppUserRepository;
import com.example.demo.exchange.repository.ExchangeMatchRepository;
import com.example.demo.exchange.repository.ExchangeRoomRepository;
import com.example.demo.exchange.repository.MatchParticipantRepository;
import com.example.demo.exchange.repository.UserPreferenceScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExchangeMatchService {

    private final ExchangeMatchRepository matchRepository;
    private final MatchParticipantRepository participantRepository;
    private final ExchangeRoomRepository roomRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;
    private final UserPreferenceScoreRepository preferenceScoreRepository;

    // 매칭 신청
    @Transactional
    public ExchangeMatchResponseDto createMatch(Long userId, Long targetUserId) {
        if (!participantRepository.findActiveMatchesByUserId(userId).isEmpty()) {
            throw new RuntimeException("이미 진행 중인 매칭이 있습니다.");
        }

        ExchangeMatch match = new ExchangeMatch("PENDING", LocalDateTime.now());
        matchRepository.save(match);

        // 신청자 저장
        MatchParticipant participant = new MatchParticipant(match, userId);
        participantRepository.save(participant);

        // 상대방도 저장
        if (targetUserId != null) {
            MatchParticipant targetParticipant = new MatchParticipant(match, targetUserId);
            participantRepository.save(targetParticipant);
            notificationService.createNotification(targetUserId, "MATCH_REQUEST", match.getId(), "MATCH");
        }

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

        participantRepository.findByExchangeMatchId(matchId)
                .ifPresent(p -> notificationService.createNotification(
                        p.getUserId(), "MATCH_ACCEPTED", room.getId(), "ROOM"));

        return new ExchangeRoomResponseDto(room);
    }

    // 매칭 조회 - 상대방 닉네임 + 카테고리 반환
    @Transactional(readOnly = true)
    public ExchangeMatchResponseDto getMatch(Long matchId) {
        ExchangeMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("매칭을 찾을 수 없습니다."));

        // 상대방(userId=1이 아닌 사람) 찾기
        MatchParticipant targetParticipant = participantRepository.findAllByExchangeMatchId(matchId)
                .stream()
                .filter(p -> !p.getUserId().equals(1L))
                .findFirst()
                .orElse(null);

        if (targetParticipant == null) {
            return new ExchangeMatchResponseDto(match, "사용자", List.of());
        }

        Long targetUserId = targetParticipant.getUserId();

        String nickname = appUserRepository.findById(targetUserId)
                .map(AppUser::getNickname)
                .orElse("사용자");

        List<String> topCategories = preferenceScoreRepository.findByUserId(targetUserId)
                .stream()
                .sorted(Comparator.comparingDouble(UserPreferenceScore::getScore).reversed())
                .limit(3)
                .map(UserPreferenceScore::getCategory)
                .collect(Collectors.toList());

        return new ExchangeMatchResponseDto(match, nickname, topCategories);
    }

    // 대기 중인 매칭 목록 조회
    @Transactional(readOnly = true)
    public List<ExchangeMatchResponseDto> getPendingMatches() {
        return matchRepository.findByStatus("PENDING")
                .stream()
                .map(match -> {
                    String nickname = participantRepository.findAllByExchangeMatchId(match.getId())
                            .stream()
                            .filter(p -> !p.getUserId().equals(1L))
                            .findFirst()
                            .flatMap(p -> appUserRepository.findById(p.getUserId()))
                            .map(AppUser::getNickname)
                            .orElse("사용자");
                    return new ExchangeMatchResponseDto(match, nickname);
                })
                .collect(Collectors.toList());
    }

    // 매칭 거절
    @Transactional
    public void rejectMatch(Long matchId) {
        ExchangeMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("매칭을 찾을 수 없습니다."));
        match.updateStatus("REJECTED");
    }

    // 내 활성 매칭 조회
    @Transactional(readOnly = true)
    public ExchangeMatchResponseDto getMyPendingMatch(Long userId) {
        List<MatchParticipant> matches = participantRepository.findActiveMatchesByUserId(userId);
        if (matches.isEmpty()) return null;
        return new ExchangeMatchResponseDto(matches.get(0).getExchangeMatch());
    }
}