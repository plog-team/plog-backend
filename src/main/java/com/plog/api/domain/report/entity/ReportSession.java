package com.plog.api.domain.report.entity;

import com.plog.api.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "report_session")
@Getter
@Setter
@NoArgsConstructor
public class ReportSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 36)
    private String threadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ReportStatus status;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Lob
    private String workingDataJson;

    @Lob
    private String reportJson;

    @Column(length = 500)
    private String errorMessage;
}
