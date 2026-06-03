package com.plog.api.domain.report.entity;

import com.plog.api.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "report_feedback")
@Getter
@Setter
@NoArgsConstructor
public class ReportFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Long userId;

    private Integer rating;

    @Lob
    private String comment;
}
