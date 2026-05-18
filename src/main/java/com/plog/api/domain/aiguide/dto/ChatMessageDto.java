package com.plog.api.domain.aiguide.dto;

import com.plog.api.domain.aiguide.ChatMessage;

import lombok.Builder;

@Builder
public record ChatMessageDto(
    Long id,
    String role,
    String content,
    Integer orderIdx
) {
    public static ChatMessageDto from(ChatMessage m) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .role(m.getRole().name())
                .content(m.getContent())
                .orderIdx(m.getOrderIdx())
                .build();
    }
}
