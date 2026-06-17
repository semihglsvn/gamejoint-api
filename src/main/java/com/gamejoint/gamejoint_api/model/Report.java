package com.gamejoint.gamejoint_api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
@EntityListeners(AuditingEntityListener.class)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many reports can be submitted by One user (the reporter)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // Many reports can target One specific review
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String reasons;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum('pending', 'reviewed', 'ignored')")
    private ReportStatus status = ReportStatus.PENDING; // Automatically defaults new reports to pending

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ReportStatus {
        PENDING,
        REVIEWED,
        IGNORED
    }
}