package com.bank.mortgage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType; // MIME type

    @Column(nullable = false)
    private Long fileSize; // in bytes

    @Column(nullable = false, length = 2048)
    private String s3PresignedUrl; // S3-style presigned URL

    @Column(nullable = false)
    private String s3Key; // S3 object key for reference

    @Column(nullable = false)
    private Instant uploadedAt;

    @Column(nullable = false)
    private Instant expiresAt; // Presigned URL expiration

    @Column(nullable = true)
    private String uploadedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
