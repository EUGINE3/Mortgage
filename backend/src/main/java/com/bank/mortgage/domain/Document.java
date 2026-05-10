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

    @Column(name = "application_id")
    private UUID applicationId;

    private String documentType;

    private String fileName;

    private String fileUrl;

    private String mimeType;

    private Long fileSize;

    private String uploadedBy;

    private Instant uploadedAt;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Integer version;
}
