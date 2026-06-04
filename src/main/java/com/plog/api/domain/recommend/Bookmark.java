package com.plog.api.domain.recommend;

import com.plog.api.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "bookmark",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "content_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content_id", nullable = false, length = 20)
    private String contentId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 200)
    private String address;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(length = 30)
    private String category;

    @Column(name = "content_type_id", length = 10)
    private String contentTypeId;

    @Builder
    private Bookmark(Long userId, String contentId, String title,
                     String address, String imageUrl,
                     String category, String contentTypeId) {
        this.userId        = userId;
        this.contentId     = contentId;
        this.title         = title;
        this.address       = address;
        this.imageUrl      = imageUrl;
        this.category      = category;
        this.contentTypeId = contentTypeId;
    }
}
