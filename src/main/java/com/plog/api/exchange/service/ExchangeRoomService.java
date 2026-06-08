package com.plog.api.exchange.service;

import com.plog.api.exchange.domain.ExchangeMatch;
import com.plog.api.exchange.domain.ExchangeRoom;
import com.plog.api.exchange.dto.ExchangeRoomResponseDto;
import com.plog.api.exchange.repository.ExchangeRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExchangeRoomService {

    private final ExchangeRoomRepository roomRepository;

    // 교환방 조회
    @Transactional(readOnly = true)
    public ExchangeRoomResponseDto getRoom(Long roomId) {
        ExchangeRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("교환방을 찾을 수 없습니다."));
        return new ExchangeRoomResponseDto(room);
    }

    // 교환방 목록 조회
    @Transactional(readOnly = true)
    public List<ExchangeRoomResponseDto> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(ExchangeRoomResponseDto::new)
                .collect(Collectors.toList());
    }

    // 교환방 종료 + 매칭 상태 변경
    @Transactional
    public ExchangeRoomResponseDto closeRoom(Long roomId) {
        ExchangeRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("교환방을 찾을 수 없습니다."));
        room.close();

        // 매칭 상태도 REJECTED로 변경
        ExchangeMatch match = room.getExchangeMatch();
        if (match != null) {
            match.updateStatus("REJECTED");
        }

        return new ExchangeRoomResponseDto(room);
    }

    // 내 활성 교환방 조회
    @Transactional(readOnly = true)
    public ExchangeRoomResponseDto getActiveRoom(Long userId) {
        List<ExchangeRoom> rooms = roomRepository.findActiveRoomsByUserId(userId);
        if (rooms.isEmpty()) return null;
        return new ExchangeRoomResponseDto(rooms.get(0));
    }
}