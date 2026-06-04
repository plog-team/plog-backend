package com.plog.api.domain.recommend;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "click_log")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClickLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content_id", nullable = false, length = 20)
    private String contentId;

    @Column(name = "content_type_id", length = 10)
    private String contentTypeId;

    @Column(length = 30)
    private String category;

    @CreatedDate
    @Column(name = "clicked_at", nullable = false, updatable = false)
    private LocalDateTime clickedAt;

    @Builder
    private ClickLog(Long userId, String contentId,
                     String contentTypeId, String category) {
        this.userId        = userId;
        this.contentId     = contentId;
        this.contentTypeId = contentTypeId;
        this.category      = category;
    }
}
