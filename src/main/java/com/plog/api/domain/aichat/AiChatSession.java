package com.plog.api.domain.aichat;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.plog.api.domain.BaseTimeEntity;
import com.plog.api.domain.user.User;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "ai_chat_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AiChatSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String type;

    @Builder
    private AiChatSession(User user, String type) {
        this.user = user;
        this.type = type;
    }

    public static AiChatSession of(User user, String type) {
        return AiChatSession.builder().user(user).type(type).build();
    }
}