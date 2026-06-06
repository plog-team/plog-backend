package com.plog.api.domain.aichat;

import jakarta.persistence.*;
import lombok.*;

import com.plog.api.domain.BaseTimeEntity;

@Getter
@Entity
@Table(name = "ai_chat_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AiChatSession session;

    @Column(nullable = false, length = 30)
    private String sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 30)
    private String messageType;

    @Builder
    private AiChatMessage(AiChatSession session, String sender, String message, String messageType) {
        this.session = session;
        this.sender = sender;
        this.message = message;
        this.messageType = messageType;
    }

    public static AiChatMessage of(AiChatSession session, String sender, String message, String messageType) {
        return AiChatMessage.builder()
                .session(session)
                .sender(sender)
                .message(message)
                .messageType(messageType)
                .build();
    }
}