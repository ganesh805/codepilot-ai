package com.codepilot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sql_optimizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SqlOptimization {

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

    @Column(name = "raw_sql", nullable = false, columnDefinition = "LONGTEXT")
    private String rawSql;

    @Column(name = "optimized_sql", nullable = false, columnDefinition = "LONGTEXT")
    private String optimizedSql;

    @Column(name = "indexing_ddl", nullable = false, columnDefinition = "LONGTEXT")
    private String indexingDdl;

    @Column(name = "performance_gain_pct", nullable = false)
    private int performanceGainPct;

    @Column(name = "analysis_summary", nullable = false, columnDefinition = "LONGTEXT")
    private String analysisSummary;

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
