package com.plog.api.domain.aichat;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plog.api.common.UserContext;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    // 세션 시작
    @PostMapping("/session")
    public ResponseEntity<?> startSession(
            @RequestParam(value = "type", defaultValue = "FREE_CHAT") String type,
            @RequestParam(value = "date", required = false) String date) {
        Map<String, Object> result = aiChatService.startSession(UserContext.get(), type, date);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    // 메시지 전송
    @PostMapping("/session/{sessionId}/message")
    public ResponseEntity<?> sendMessage(
            @PathVariable("sessionId") Long sessionId,
            @RequestParam("message") String message) {
        Map<String, Object> result = aiChatService.sendMessage(sessionId, UserContext.get(), message);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    // 메시지 목록 조회
    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable("sessionId") Long sessionId) {
        return ResponseEntity.ok(Map.of("success", true, "data", aiChatService.getMessages(sessionId)));
    }

    // 세션 종료 (제목 + 감정 저장)
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<?> endSession(@PathVariable("sessionId") Long sessionId) {
        aiChatService.endSession(sessionId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // 세션 목록 조회 (대화 히스토리 목록)
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions() {
        List<Map<String, Object>> result = aiChatService.getSessions(UserContext.get());
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    // 세션 상세 조회 (이어하기)
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSessionDetail(@PathVariable("sessionId") Long sessionId) {
        Map<String, Object> result = aiChatService.getSessionDetail(sessionId);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }
}