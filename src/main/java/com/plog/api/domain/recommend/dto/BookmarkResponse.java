package com.plog.api.domain.recommend.dto;

import com.plog.api.domain.recommend.Bookmark;
import java.time.LocalDateTime;

public record BookmarkResponse(
        String contentId,
        String title,
        String address,
        String imageUrl,
        String category,
        String contentTypeId,
        LocalDateTime savedAt
) {
    public static BookmarkResponse from(Bookmark b) {
        return new BookmarkResponse(
                b.getContentId(), b.getTitle(), b.getAddress(),
                b.getImageUrl(), b.getCategory(), b.getContentTypeId(),
                b.getCreatedAt());
    }
}
