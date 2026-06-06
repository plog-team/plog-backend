package com.plog.api.domain.aichat;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    // 세션 시작 (type: DIARY_ASSIST 또는 FREE_CHAT)
    @PostMapping("/session")
    public ResponseEntity<?> startSession(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "type", defaultValue = "FREE_CHAT") String type,
            @RequestParam(value = "date", required = false) String date) {
        Map<String, Object> result = aiChatService.startSession(userId, type, date);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }@
    
    PostMapping("/session/{sessionId}/message")
    public ResponseEntity<?> sendMessage(
            @PathVariable("sessionId") Long sessionId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("message") String message) {
        Map<String, Object> result = aiChatService.sendMessage(sessionId, userId, message);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
}

    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable("sessionId") Long sessionId) {
        return ResponseEntity.ok(Map.of("success", true, "data", aiChatService.getMessages(sessionId)));
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> endSession(@PathVariable("sessionId") Long sessionId) {
        aiChatService.endSession(sessionId);
        return ResponseEntity.ok().build();
    }
}