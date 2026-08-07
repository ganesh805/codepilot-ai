package com.codepilot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "code_optimizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeOptimizer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private CodeRepository repository;

    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @Column(name = "optimization_level", nullable = false, length = 255)
    private String optimizationLevel;

    @Column(name = "raw_code", nullable = false, columnDefinition = "LONGTEXT")
    private String rawCode;

    @Column(name = "optimized_code", nullable = false, columnDefinition = "LONGTEXT")
    private String optimizedCode;

    @Column(name = "time_complexity_before", length = 100)
    private String timeComplexityBefore;

    @Column(name = "time_complexity_after", length = 100)
    private String timeComplexityAfter;

    @Column(name = "space_complexity_before", length = 100)
    private String spaceComplexityBefore;

    @Column(name = "space_complexity_after", length = 100)
    private String spaceComplexityAfter;

    @Column(name = "full_report_markdown", nullable = false, columnDefinition = "LONGTEXT")
    private String fullReportMarkdown;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
}
