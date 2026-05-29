package com.plog.api.domain.diary;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plog.api.common.exception.BadRequestException;
import com.plog.api.common.exception.NotFoundException;
import com.plog.api.domain.diary.dto.DiaryEmojiDecorationRequest;
import com.plog.api.domain.diary.dto.DiaryEmojiDecorationResponse;
import com.plog.api.domain.diary.dto.DiaryLineCommentRequest;
import com.plog.api.domain.diary.dto.DiaryLineCommentResponse;
import com.plog.api.domain.diary.dto.DiaryLineCommentUpdateRequest;
import com.plog.api.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiaryInteractionService {

    private final DiaryRepository diaryRepository;
    private final DiaryLineCommentRepository commentRepository;
    private final DiaryEmojiDecorationRepository decorationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DiaryLineCommentResponse> listComments(long userId, long diaryId, Integer lineIndex) {
        ensureDiaryVisible(userId, diaryId);
        List<DiaryLineComment> comments = lineIndex == null
                ? commentRepository.findAllByDiaryIdAndDeletedFalseOrderByLineIndexAscIdAsc(diaryId)
                : commentRepository.findAllByDiaryIdAndLineIndexAndDeletedFalseOrderByIdAsc(diaryId, lineIndex);
        return comments.stream()
                .map(comment -> DiaryLineCommentResponse.from(comment, authorName(comment.getUserId())))
                .toList();
    }

    @Transactional
    public DiaryLineCommentResponse createComment(long userId, long diaryId, DiaryLineCommentRequest req) {
        ensureDiaryVisible(userId, diaryId);
        DiaryLineComment comment = commentRepository.save(DiaryLineComment.builder()
                .diaryId(diaryId)
                .userId(userId)
                .lineIndex(req.lineIndex())
                .content(req.content().trim())
                .build());
        return DiaryLineCommentResponse.from(comment, authorName(userId));
    }

    @Transactional
    public DiaryLineCommentResponse updateComment(long userId, long diaryId, long commentId,
                                                  DiaryLineCommentUpdateRequest req) {
        ensureDiaryVisible(userId, diaryId);
        DiaryLineComment comment = commentRepository.findByIdAndDiaryIdAndDeletedFalse(commentId, diaryId)
                .orElseThrow(() -> new NotFoundException("Comment not found id=" + commentId));
        ensureOwner(userId, comment.getUserId(), "comment");
        comment.update(req.content().trim());
        return DiaryLineCommentResponse.from(comment, authorName(userId));
    }

    @Transactional
    public void deleteComment(long userId, long diaryId, long commentId) {
        ensureDiaryVisible(userId, diaryId);
        DiaryLineComment comment = commentRepository.findByIdAndDiaryIdAndDeletedFalse(commentId, diaryId)
                .orElseThrow(() -> new NotFoundException("Comment not found id=" + commentId));
        ensureOwner(userId, comment.getUserId(), "comment");
        comment.delete();
    }

    @Transactional(readOnly = true)
    public List<DiaryEmojiDecorationResponse> listDecorations(long userId, long diaryId) {
        ensureDiaryVisible(userId, diaryId);
        return decorationRepository.findAllByDiaryIdAndDeletedFalseOrderByIdAsc(diaryId).stream()
                .map(decoration -> DiaryEmojiDecorationResponse.from(decoration, authorName(decoration.getUserId())))
                .toList();
    }

    @Transactional
    public DiaryEmojiDecorationResponse createDecoration(long userId, long diaryId,
                                                         DiaryEmojiDecorationRequest req) {
        ensureDiaryVisible(userId, diaryId);
        DiaryEmojiDecoration decoration = decorationRepository.save(DiaryEmojiDecoration.builder()
                .diaryId(diaryId)
                .userId(userId)
                .emoji(req.emoji().trim())
                .xRatio(req.xRatio())
                .yRatio(req.yRatio())
                .scale(req.scale())
                .rotation(req.rotation())
                .build());
        return DiaryEmojiDecorationResponse.from(decoration, authorName(userId));
    }

    @Transactional
    public DiaryEmojiDecorationResponse updateDecoration(long userId, long diaryId, long decorationId,
                                                         DiaryEmojiDecorationRequest req) {
        ensureDiaryVisible(userId, diaryId);
        DiaryEmojiDecoration decoration = decorationRepository.findByIdAndDiaryIdAndDeletedFalse(decorationId, diaryId)
                .orElseThrow(() -> new NotFoundException("Decoration not found id=" + decorationId));
        ensureOwner(userId, decoration.getUserId(), "decoration");
        decoration.update(req.emoji().trim(), req.xRatio(), req.yRatio(), req.scale(), req.rotation());
        return DiaryEmojiDecorationResponse.from(decoration, authorName(userId));
    }

    @Transactional
    public void deleteDecoration(long userId, long diaryId, long decorationId) {
        ensureDiaryVisible(userId, diaryId);
        DiaryEmojiDecoration decoration = decorationRepository.findByIdAndDiaryIdAndDeletedFalse(decorationId, diaryId)
                .orElseThrow(() -> new NotFoundException("Decoration not found id=" + decorationId));
        ensureOwner(userId, decoration.getUserId(), "decoration");
        decoration.delete();
    }

    private Diary ensureDiaryVisible(long userId, long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NotFoundException("Diary not found id=" + diaryId));
        if (diary.isSecret() && !diary.getUserId().equals(userId)) {
            throw new BadRequestException("Secret diary is not visible");
        }
        return diary;
    }

    private void ensureOwner(long currentUserId, long ownerId, String resourceName) {
        if (currentUserId != ownerId) {
            throw new BadRequestException("Current user cannot modify this " + resourceName);
        }
    }

    private String authorName(long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getName())
                .orElse("User " + userId);
    }
}
