package com.example.demo.exchange.controller;

import com.example.demo.exchange.dto.ExchangeRoomResponseDto;
import com.example.demo.exchange.service.ExchangeRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exchange/rooms")
@RequiredArgsConstructor
public class ExchangeRoomController {

    private final ExchangeRoomService roomService;

    // 교환방 조회
    @GetMapping("/{roomId}")
    public ResponseEntity<ExchangeRoomResponseDto> getRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getRoom(roomId));
    }

    // 교환방 목록 조회
    @GetMapping
    public ResponseEntity<List<ExchangeRoomResponseDto>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    // 교환방 종료
    @PatchMapping("/{roomId}/close")
    public ResponseEntity<ExchangeRoomResponseDto> closeRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.closeRoom(roomId));
    }
}