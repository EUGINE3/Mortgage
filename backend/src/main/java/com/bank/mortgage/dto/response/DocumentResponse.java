package com.bank.mortgage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private UUID id;

    private UUID applicationId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String s3PresignedUrl;

    private Instant uploadedAt;

    private Instant expiresAt;

    private String uploadedBy;

    private Instant createdAt;

    private Instant updatedAt;
}
