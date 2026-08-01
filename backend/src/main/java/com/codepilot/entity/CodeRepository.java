package com.codepilot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "repositories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String owner;

    @Column(name = "git_url", length = 255)
    private String gitUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_type", nullable = false, length = 20)
    private ImportType importType;

    @Column(name = "storage_path", nullable = false, length = 255)
    private String storagePath;

    @Column(name = "default_branch", length = 50)
    @Builder.Default
    private String defaultBranch = "main";

    @Column(name = "file_count")
    @Builder.Default
    private int fileCount = 0;

    @Column(name = "total_size_bytes")
    @Builder.Default
    private long totalSizeBytes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RepositoryStatus status = RepositoryStatus.READY;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
}
