package com.plog.api.domain.report.entity;

import com.plog.api.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "report_interrupt")
@Getter
@Setter
@NoArgsConstructor
public class ReportInterrupt extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Long diaryId;

    @Column(nullable = false)
    private LocalDate diaryDate;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    @Column(length = 200)
    private String answer;

    @Column(nullable = false)
    private int orderIdx;
}
