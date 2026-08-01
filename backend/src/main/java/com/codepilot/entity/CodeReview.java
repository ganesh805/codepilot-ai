package com.codepilot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "code_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeReview {

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

    @Column(name = "pr_title", nullable = false, length = 200)
    private String prTitle;

    @Column(name = "git_diff", nullable = false, columnDefinition = "LONGTEXT")
    private String gitDiff;

    @Column(name = "quality_score", nullable = false)
    private int qualityScore;

    @Column(name = "security_issues_count", nullable = false)
    private int securityIssuesCount;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String summary;

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
