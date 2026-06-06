package com.plog.api.domain.recommend;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@Table(name = "user_preference")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    // 콤마 구분 저장
    @Column(name = "preferred_categories", length = 100)
    private String preferredCategories;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private UserPreference(Long userId, String preferredCategories) {
        this.userId               = userId;
        this.preferredCategories  = preferredCategories;
    }

    public List<String> getCategoryList() {
        if (preferredCategories == null || preferredCategories.isBlank())
            return List.of();
        return List.of(preferredCategories.split(","));
    }

    public void update(List<String> categories) {
        this.preferredCategories = String.join(",", categories);
    }
}
