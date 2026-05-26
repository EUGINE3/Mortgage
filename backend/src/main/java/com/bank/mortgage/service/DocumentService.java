package com.bank.mortgage.service;

import com.bank.mortgage.domain.Document;
import com.bank.mortgage.dto.request.DocumentUploadRequest;
import com.bank.mortgage.dto.response.DocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DocumentService {

    DocumentResponse generatePresignedUrl(UUID applicationId, DocumentUploadRequest request);

    DocumentResponse getDocument(UUID documentId);

    Page<DocumentResponse> listDocuments(UUID applicationId, Pageable pageable);

    void deleteDocument(UUID documentId);

    boolean isDocumentValid(UUID documentId);
}
