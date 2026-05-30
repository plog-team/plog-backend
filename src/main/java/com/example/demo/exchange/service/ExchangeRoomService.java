package com.example.demo.exchange.service;

import com.example.demo.exchange.domain.ExchangeRoom;
import com.example.demo.exchange.dto.ExchangeRoomResponseDto;
import com.example.demo.exchange.repository.ExchangeRoomRepository;
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

    // 교환방 종료
    @Transactional
    public ExchangeRoomResponseDto closeRoom(Long roomId) {
        ExchangeRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("교환방을 찾을 수 없습니다."));

        room.close();

        return new ExchangeRoomResponseDto(room);
    }
}