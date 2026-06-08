package com.plog.api.domain.aichat;



import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.plog.api.domain.BaseTimeEntity;
import com.plog.api.domain.user.User;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
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

    private String title;
    private Boolean isDiary = false;
    private LocalDate diaryDate;
    private String emotion;
    private Float emotionScore;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<AiChatMessage> messages = new ArrayList<>();

    @Builder
    private AiChatSession(User user, String type) {
        this.user = user;
        this.type = type;
    }

    public static AiChatSession of(User user, String type) {
        return AiChatSession.builder().user(user).type(type).build();
    }
}