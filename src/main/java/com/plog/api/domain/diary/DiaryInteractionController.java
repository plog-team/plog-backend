package com.plog.api.domain.diary;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plog.api.common.UserContext;
import com.plog.api.domain.diary.dto.DiaryEmojiDecorationRequest;
import com.plog.api.domain.diary.dto.DiaryEmojiDecorationResponse;
import com.plog.api.domain.diary.dto.DiaryLineCommentRequest;
import com.plog.api.domain.diary.dto.DiaryLineCommentResponse;
import com.plog.api.domain.diary.dto.DiaryLineCommentUpdateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/diaries/{diaryId}")
@RequiredArgsConstructor
public class DiaryInteractionController {

    private final DiaryInteractionService interactionService;

    @GetMapping("/comments")
    public List<DiaryLineCommentResponse> listComments(
            @PathVariable long diaryId,
            @RequestParam(required = false) Integer lineIndex) {
        return interactionService.listComments(UserContext.get(), diaryId, lineIndex);
    }

    @PostMapping("/comments")
    public DiaryLineCommentResponse createComment(
            @PathVariable long diaryId,
            @Valid @RequestBody DiaryLineCommentRequest req) {
        return interactionService.createComment(UserContext.get(), diaryId, req);
    }

    @PutMapping("/comments/{commentId}")
    public DiaryLineCommentResponse updateComment(
            @PathVariable long diaryId,
            @PathVariable long commentId,
            @Valid @RequestBody DiaryLineCommentUpdateRequest req) {
        return interactionService.updateComment(UserContext.get(), diaryId, commentId, req);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable long diaryId,
            @PathVariable long commentId) {
        interactionService.deleteComment(UserContext.get(), diaryId, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/decorations")
    public List<DiaryEmojiDecorationResponse> listDecorations(@PathVariable long diaryId) {
        return interactionService.listDecorations(UserContext.get(), diaryId);
    }

    @PostMapping("/decorations")
    public DiaryEmojiDecorationResponse createDecoration(
            @PathVariable long diaryId,
            @Valid @RequestBody DiaryEmojiDecorationRequest req) {
        return interactionService.createDecoration(UserContext.get(), diaryId, req);
    }

    @PutMapping("/decorations/{decorationId}")
    public DiaryEmojiDecorationResponse updateDecoration(
            @PathVariable long diaryId,
            @PathVariable long decorationId,
            @Valid @RequestBody DiaryEmojiDecorationRequest req) {
        return interactionService.updateDecoration(UserContext.get(), diaryId, decorationId, req);
    }

    @DeleteMapping("/decorations/{decorationId}")
    public ResponseEntity<Void> deleteDecoration(
            @PathVariable long diaryId,
            @PathVariable long decorationId) {
        interactionService.deleteDecoration(UserContext.get(), diaryId, decorationId);
        return ResponseEntity.noContent().build();
    }
}
