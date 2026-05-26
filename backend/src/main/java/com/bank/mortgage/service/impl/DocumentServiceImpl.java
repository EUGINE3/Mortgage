package com.bank.mortgage.service.impl;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.domain.Document;
import com.bank.mortgage.dto.request.DocumentUploadRequest;
import com.bank.mortgage.dto.response.DocumentResponse;
import com.bank.mortgage.exception.NotFoundException;
import com.bank.mortgage.exception.UnauthorizedException;
import com.bank.mortgage.mapper.DocumentMapper;
import com.bank.mortgage.repository.ApplicationRepository;
import com.bank.mortgage.repository.DocumentRepository;
import com.bank.mortgage.service.DocumentService;
import com.bank.mortgage.util.S3PresignedUrlGenerator;
import com.bank.mortgage.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentMapper documentMapper;
    private final S3PresignedUrlGenerator s3Generator;
    private final SecurityUtil securityUtil;

    @Override
    public DocumentResponse generatePresignedUrl(UUID applicationId, DocumentUploadRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        // Verify authorization
        if (securityUtil.isApplicant() && !securityUtil.isCurrentUserOwner(application.getApplicant())) {
            throw new UnauthorizedException("You can only upload documents to your own applications");
        }

        // Generate S3 key and presigned URL
        String s3Key = generateS3Key(applicationId, request.getFileName());
        String presignedUrl = s3Generator.generatePresignedUrl(s3Key, request.getFileType(), 3600);

        Document document = Document.builder()
                .id(UUID.randomUUID())
                .application(application)
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .fileSize(request.getFileSize())
                .s3PresignedUrl(presignedUrl)
                .s3Key(s3Key)
                .uploadedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .uploadedBy(request.getUploadedBy() != null ? request.getUploadedBy() : securityUtil.getCurrentUser().getEmail())
                .build();

        Document saved = documentRepository.save(document);
        log.info("Generated presigned URL for document {} with S3 key {}", saved.getId(), s3Key);

        return documentMapper.toResponse(saved);
    }

    @Override
    public DocumentResponse getDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        // Verify authorization
        if (securityUtil.isApplicant() && !securityUtil.isCurrentUserOwner(document.getApplication().getApplicant())) {
            throw new UnauthorizedException("You can only access documents from your own applications");
        }

        return documentMapper.toResponse(document);
    }

    @Override
    public Page<DocumentResponse> listDocuments(UUID applicationId, Pageable pageable) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        // Verify authorization
        if (securityUtil.isApplicant() && !securityUtil.isCurrentUserOwner(application.getApplicant())) {
            throw new UnauthorizedException("You can only view documents from your own applications");
        }

        Page<Document> documents = documentRepository.findByApplication(application, pageable);
        return documents.map(documentMapper::toResponse);
    }

    @Override
    public void deleteDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        // Verify authorization
        if (securityUtil.isApplicant() && !securityUtil.isCurrentUserOwner(document.getApplication().getApplicant())) {
            throw new UnauthorizedException("You can only delete documents from your own applications");
        }

        documentRepository.delete(document);
        s3Generator.deleteObject(document.getS3Key());
        log.info("Deleted document {} with S3 key {}", documentId, document.getS3Key());
    }

    @Override
    public boolean isDocumentValid(UUID documentId) {
        return documentRepository.findById(documentId)
                .map(doc -> doc.getExpiresAt().isAfter(Instant.now()))
                .orElse(false);
    }

    private String generateS3Key(UUID applicationId, String fileName) {
        return String.format("applications/%s/documents/%s-%s", 
                applicationId, 
                UUID.randomUUID(), 
                fileName);
    }
}
