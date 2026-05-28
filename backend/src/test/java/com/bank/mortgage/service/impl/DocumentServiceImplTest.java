package com.bank.mortgage.service.impl;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.domain.Document;
import com.bank.mortgage.domain.User;
import com.bank.mortgage.dto.request.DocumentUploadRequest;
import com.bank.mortgage.dto.response.DocumentResponse;
import com.bank.mortgage.exception.NotFoundException;
import com.bank.mortgage.exception.UnauthorizedException;
import com.bank.mortgage.mapper.DocumentMapper;
import com.bank.mortgage.repository.ApplicationRepository;
import com.bank.mortgage.repository.DocumentRepository;
import com.bank.mortgage.util.S3PresignedUrlGenerator;
import com.bank.mortgage.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private S3PresignedUrlGenerator s3Generator;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private DocumentServiceImpl service;

    private UUID userId;
    private UUID applicationId;
    private UUID documentId;
    private User applicant;
    private User creditOfficer;
    private Application application;
    private Document document;
    private DocumentUploadRequest uploadRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        applicant = User.builder()
                .id(userId)
                .email("applicant@example.com")
                .role("APPLICANT")
                .build();

        creditOfficer = User.builder()
                .id(UUID.randomUUID())
                .email("officer@example.com")
                .role("CREDIT_OFFICER")
                .build();

        application = Application.builder()
                .id(applicationId)
                .applicant(applicant)
                .build();

        uploadRequest = DocumentUploadRequest.builder()
                .fileName("passport.pdf")
                .fileType("application/pdf")
                .fileSize(1024L)
                .build();

        document = Document.builder()
                .id(documentId)
                .application(application)
                .fileName("passport.pdf")
                .fileType("application/pdf")
                .fileSize(1024L)
                .s3Key("applications/" + applicationId + "/documents/doc-123-passport.pdf")
                .s3PresignedUrl("https://s3.amazonaws.com/presigned")
                .uploadedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .uploadedBy("applicant@example.com")
                .build();
    }

    // =====================================================
    // GENERATE PRESIGNED URL TESTS
    // =====================================================

    @Test
    @DisplayName("Should generate presigned URL for valid request")
    void generatePresignedUrl_withValidRequest_shouldReturnDocumentResponse() {
        // Given
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.getCurrentUser()).thenReturn(applicant);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);
        when(s3Generator.generatePresignedUrl(anyString(), eq("application/pdf"), eq(3600)))
                .thenReturn("https://s3.amazonaws.com/presigned");
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toResponse(document)).thenReturn(buildDocumentResponse(document));

        // When
        DocumentResponse response = service.generatePresignedUrl(applicationId, uploadRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFileName()).isEqualTo("passport.pdf");
        assertThat(response.getFileType()).isEqualTo("application/pdf");
        assertThat(response.getS3PresignedUrl()).isEqualTo("https://s3.amazonaws.com/presigned");

        verify(documentRepository).save(any(Document.class));
        verify(s3Generator).generatePresignedUrl(anyString(), eq("application/pdf"), eq(3600));
    }

    @Test
    @DisplayName("Should throw NotFoundException when application doesn't exist")
    void generatePresignedUrl_withApplicationNotFound_shouldThrowNotFoundException() {
        // Given
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.generatePresignedUrl(applicationId, uploadRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application not found");

        verify(documentRepository, never()).save(any());
        verify(s3Generator, never()).generatePresignedUrl(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when applicant accesses other's application")
    void generatePresignedUrl_applicantAccessingOtherApplications_shouldThrowUnauthorizedException() {
        // Given
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.generatePresignedUrl(applicationId, uploadRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only upload documents to your own applications");

        verify(documentRepository, never()).save(any());
        verify(s3Generator, never()).generatePresignedUrl(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Should allow credit officer to upload documents")
    void generatePresignedUrl_creditOfficerCanUpload_shouldSucceed() {
        // Given
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(false);
        when(securityUtil.getCurrentUser()).thenReturn(creditOfficer);
        when(s3Generator.generatePresignedUrl(anyString(), eq("application/pdf"), eq(3600)))
                .thenReturn("https://s3.amazonaws.com/presigned");
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toResponse(document)).thenReturn(buildDocumentResponse(document));

        // When
        DocumentResponse response = service.generatePresignedUrl(applicationId, uploadRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFileName()).isEqualTo("passport.pdf");

        verify(documentRepository).save(any(Document.class));
        verify(s3Generator).generatePresignedUrl(anyString(), eq("application/pdf"), eq(3600));
    }

    @Test
    @DisplayName("Should use custom uploadedBy when provided")
    void generatePresignedUrl_withCustomUploadedBy_shouldUseProvidedValue() {
        // Given
        uploadRequest.setUploadedBy("custom@example.com");

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(false);
        //when(securityUtil.getCurrentUser()).thenReturn(creditOfficer);
        when(s3Generator.generatePresignedUrl(anyString(), anyString(), anyInt()))
                .thenReturn("https://s3.amazonaws.com/presigned");

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        when(documentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(documentMapper.toResponse(any(Document.class)))
                .thenAnswer(inv -> buildDocumentResponse(inv.getArgument(0)));

        // When
        DocumentResponse response = service.generatePresignedUrl(applicationId, uploadRequest);

        // Then
        assertThat(response.getUploadedBy()).isEqualTo("custom@example.com");

        Document savedDoc = captor.getValue();
        assertThat(savedDoc.getUploadedBy()).isEqualTo("custom@example.com");

        verify(s3Generator).generatePresignedUrl(anyString(), anyString(), anyInt());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    @DisplayName("Should generate unique S3 key for each document")
    void generatePresignedUrl_shouldGenerateValidS3Key() {
        // Given
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(false);
        when(securityUtil.getCurrentUser()).thenReturn(creditOfficer);
        when(s3Generator.generatePresignedUrl(anyString(), anyString(), anyInt()))
                .thenReturn("https://s3.amazonaws.com/presigned");

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        when(documentRepository.save(captor.capture())).thenReturn(document);
        when(documentMapper.toResponse(any(Document.class))).thenReturn(buildDocumentResponse(document));

        // When
        service.generatePresignedUrl(applicationId, uploadRequest);

        // Then
        Document savedDoc = captor.getValue();
        assertThat(savedDoc.getS3Key()).contains("applications/" + applicationId + "/documents/");
        assertThat(savedDoc.getS3Key()).contains("passport.pdf");

        verify(s3Generator).generatePresignedUrl(anyString(), anyString(), anyInt());
        verify(documentRepository).save(any(Document.class));
    }

    // =====================================================
    // GET DOCUMENT TESTS
    // =====================================================

    @Test
    @DisplayName("Should return document when found and authorized")
    void getDocument_withValidDocumentId_shouldReturnDocument() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);
        when(documentMapper.toResponse(document)).thenReturn(buildDocumentResponse(document));

        // When
        DocumentResponse response = service.getDocument(documentId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(documentId);
        assertThat(response.getFileName()).isEqualTo("passport.pdf");

        verify(documentRepository).findById(documentId);
        verify(documentMapper).toResponse(document);
    }

    @Test
    @DisplayName("Should throw NotFoundException when document doesn't exist")
    void getDocument_withInvalidDocumentId_shouldThrowNotFoundException() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.getDocument(documentId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Document not found");

        verify(documentRepository).findById(documentId);
        verify(documentMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when applicant accesses others document")
    void getDocument_applicantAccessingOthersDocument_shouldThrowUnauthorizedException() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.getDocument(documentId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only access documents from your own applications");

        verify(documentRepository).findById(documentId);
        verify(documentMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should allow credit officer to access any document")
    void getDocument_creditOfficerCanAccess_shouldSucceed() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(securityUtil.isApplicant()).thenReturn(false);
        when(documentMapper.toResponse(document)).thenReturn(buildDocumentResponse(document));

        // When
        DocumentResponse response = service.getDocument(documentId);

        // Then
        assertThat(response).isNotNull();
        verify(documentRepository).findById(documentId);
        verify(documentMapper).toResponse(document);
    }

    // =====================================================
    // LIST DOCUMENTS TESTS
    // =====================================================

    @Test
    @DisplayName("Should return page of documents for valid application")
    void listDocuments_withValidApplicationId_shouldReturnPageOfDocuments() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Document document2 = copyDocument(document, "invoice.pdf", UUID.randomUUID(), applicationId);

        Page<Document> documents = new PageImpl<>(List.of(document, document2), pageable, 2);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);
        when(documentRepository.findByApplication(application, pageable)).thenReturn(documents);
        when(documentMapper.toResponse(document)).thenReturn(buildDocumentResponse(document));
        when(documentMapper.toResponse(document2)).thenReturn(buildDocumentResponse(document2));

        // When
        Page<DocumentResponse> response = service.listDocuments(applicationId, pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);

        verify(documentRepository).findByApplication(application, pageable);
        verify(documentMapper, times(2)).toResponse(any(Document.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when application doesn't exist")
    void listDocuments_withApplicationNotFound_shouldThrowNotFoundException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.listDocuments(applicationId, pageable))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application not found");

        verify(applicationRepository).findById(applicationId);
        verify(documentRepository, never()).findByApplication(any(), any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when applicant accesses others application")
    void listDocuments_applicantAccessingOthersApplication_shouldThrowUnauthorizedException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.listDocuments(applicationId, pageable))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only view documents from your own applications");

        verify(applicationRepository).findById(applicationId);
        verify(documentRepository, never()).findByApplication(any(), any());
    }

    @Test
    @DisplayName("Should allow credit officer to list any application's documents")
    void listDocuments_creditOfficerCanListDocuments_shouldSucceed() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> documents = new PageImpl<>(List.of(document), pageable, 1);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(false);
        when(documentRepository.findByApplication(application, pageable)).thenReturn(documents);
        when(documentMapper.toResponse(document)).thenReturn(buildDocumentResponse(document));

        // When
        Page<DocumentResponse> response = service.listDocuments(applicationId, pageable);

        // Then
        assertThat(response.getContent()).hasSize(1);
        verify(documentRepository).findByApplication(application, pageable);
        verify(documentMapper).toResponse(document);
    }

    @Test
    @DisplayName("Should return empty page when no documents exist")
    void listDocuments_emptyResult_shouldReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> documents = new PageImpl<>(List.of(), pageable, 0);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);
        when(documentRepository.findByApplication(application, pageable)).thenReturn(documents);

        // When
        Page<DocumentResponse> response = service.listDocuments(applicationId, pageable);

        // Then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);

        verify(documentRepository).findByApplication(application, pageable);
        verify(documentMapper, never()).toResponse(any());
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 10, 20 })
    @DisplayName("Should handle various page sizes correctly")
    void listDocuments_withDifferentPageSizes_shouldRespectPageSize(int pageSize) {
        // Given
        Pageable pageable = PageRequest.of(0, pageSize);
        List<Document> docList = Collections.nCopies(pageSize, document);
        Page<Document> documents = new PageImpl<>(docList, pageable, pageSize);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(false);
        when(documentRepository.findByApplication(application, pageable)).thenReturn(documents);
        when(documentMapper.toResponse(any(Document.class)))
                .thenAnswer(inv -> buildDocumentResponse(inv.getArgument(0)));

        // When
        Page<DocumentResponse> response = service.listDocuments(applicationId, pageable);

        // Then
        assertThat(response.getContent()).hasSize(pageSize);
        assertThat(response.getPageable().getPageSize()).isEqualTo(pageSize);
    }

    @Test
    @DisplayName("Should support sorting by upload date")
    void listDocuments_withSorting_shouldRespectSortOrder() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "uploadedAt"));

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(false);
        when(documentRepository.findByApplication(application, pageable))
                .thenReturn(new PageImpl<>(List.of(document), pageable, 1));
        when(documentMapper.toResponse(document)).thenReturn(buildDocumentResponse(document));

        // When
        Page<DocumentResponse> response = service.listDocuments(applicationId, pageable);

        // Then
        assertThat(response).isNotNull();
        verify(documentRepository).findByApplication(application, pageable);
    }

    // =====================================================
    // DELETE DOCUMENT TESTS
    // =====================================================

    @Test
    @DisplayName("Should delete document successfully")
    void deleteDocument_withValidDocumentId_shouldDeleteSuccessfully() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(true);

        // When
        service.deleteDocument(documentId);

        // Then
        verify(documentRepository).delete(document);
        verify(s3Generator).deleteObject(document.getS3Key());
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting non-existent document")
    void deleteDocument_withInvalidDocumentId_shouldThrowNotFoundException() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.deleteDocument(documentId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Document not found");

        verify(documentRepository, never()).delete(any());
        verify(s3Generator, never()).deleteObject(anyString());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when applicant deletes others document")
    void deleteDocument_applicantDeletingOthersDocument_shouldThrowUnauthorizedException() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(securityUtil.isApplicant()).thenReturn(true);
        when(securityUtil.isCurrentUserOwner(applicant)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.deleteDocument(documentId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only delete documents from your own applications");

        verify(documentRepository, never()).delete(any());
        verify(s3Generator, never()).deleteObject(anyString());
    }

    @Test
    @DisplayName("Should allow credit officer to delete any document")
    void deleteDocument_creditOfficerCanDelete_shouldSucceed() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(securityUtil.isApplicant()).thenReturn(false);

        // When
        service.deleteDocument(documentId);

        // Then
        verify(documentRepository).delete(document);
        verify(s3Generator).deleteObject(document.getS3Key());
    }

    @Test
    @DisplayName("Should call S3 delete with correct key")
    void deleteDocument_shouldCallS3GeneratorWithCorrectKey() {
        // Given
        String expectedS3Key = "applications/" + applicationId + "/documents/doc-123-passport.pdf";
        Document docWithSpecificKey = Document.builder()
                .id(documentId)
                .application(application)
                .fileName("passport.pdf")
                .fileType("application/pdf")
                .fileSize(1024L)
                .s3Key(expectedS3Key)
                .s3PresignedUrl("https://s3.amazonaws.com/presigned")
                .uploadedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .uploadedBy("applicant@example.com")
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(docWithSpecificKey));
        when(securityUtil.isApplicant()).thenReturn(false);

        // When
        service.deleteDocument(documentId);

        // Then
        verify(s3Generator).deleteObject(expectedS3Key);
    }

    // =====================================================
    // IS DOCUMENT VALID TESTS
    // =====================================================

    @Test
    @DisplayName("Should return true for valid (non-expired) document")
    void isDocumentValid_withValidDocument_shouldReturnTrue() {
        // Given
        Document validDocument = Document.builder()
                .id(documentId)
                .application(application)
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .s3Key(document.getS3Key())
                .s3PresignedUrl(document.getS3PresignedUrl())
                .uploadedAt(document.getUploadedAt())
                .expiresAt(Instant.now().plusSeconds(3600))
                .uploadedBy(document.getUploadedBy())
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(validDocument));

        // When
        boolean result = service.isDocumentValid(documentId);

        // Then
        assertThat(result).isTrue();
        verify(documentRepository).findById(documentId);
    }

    @Test
    @DisplayName("Should return false for expired document")
    void isDocumentValid_withExpiredDocument_shouldReturnFalse() {
        // Given
        Document expiredDocument = Document.builder()
                .id(documentId)
                .application(application)
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .s3Key(document.getS3Key())
                .s3PresignedUrl(document.getS3PresignedUrl())
                .uploadedAt(document.getUploadedAt())
                .expiresAt(Instant.now().minusSeconds(1))
                .uploadedBy(document.getUploadedBy())
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(expiredDocument));

        // When
        boolean result = service.isDocumentValid(documentId);

        // Then
        assertThat(result).isFalse();
        verify(documentRepository).findById(documentId);
    }

    @Test
    @DisplayName("Should return false for non-existent document")
    void isDocumentValid_withNonExistentDocument_shouldReturnFalse() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // When
        boolean result = service.isDocumentValid(documentId);

        // Then
        assertThat(result).isFalse();
        verify(documentRepository).findById(documentId);
    }

    @Test
    @DisplayName("Should return false when document expires exactly now")
    void isDocumentValid_withExpiringAtCurrentTime_shouldReturnFalse() {
        // Given
        Instant now = Instant.now();
        Document expiringNow = Document.builder()
                .id(documentId)
                .application(application)
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .s3Key(document.getS3Key())
                .s3PresignedUrl(document.getS3PresignedUrl())
                .uploadedAt(document.getUploadedAt())
                .expiresAt(now)
                .uploadedBy(document.getUploadedBy())
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(expiringNow));

        // When
        boolean result = service.isDocumentValid(documentId);

        // Then
        assertThat(result).isFalse();
        verify(documentRepository).findById(documentId);
    }

    @Test
    @DisplayName("Should return true for document expiring in the future")
    void isDocumentValid_withExpiringInFuture_shouldReturnTrue() {
        // Given
        Document expiringInFuture = Document.builder()
                .id(documentId)
                .application(application)
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .s3Key(document.getS3Key())
                .s3PresignedUrl(document.getS3PresignedUrl())
                .uploadedAt(document.getUploadedAt())
                .expiresAt(Instant.now().plusSeconds(7200))
                .uploadedBy(document.getUploadedBy())
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(expiringInFuture));

        // When
        boolean result = service.isDocumentValid(documentId);

        // Then
        assertThat(result).isTrue();
        verify(documentRepository).findById(documentId);
    }

    // =====================================================
    // EDGE CASE TESTS
    // =====================================================

    @Test
    @DisplayName("Should handle null file name in upload request")
    void generatePresignedUrl_withNullFileName_shouldStillGenerateKey() {
        // Given
        uploadRequest.setFileName(null);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(false);
        when(securityUtil.getCurrentUser()).thenReturn(creditOfficer);
        when(s3Generator.generatePresignedUrl(anyString(), anyString(), anyInt()))
                .thenReturn("https://s3.amazonaws.com/presigned");

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        when(documentRepository.save(captor.capture())).thenReturn(document);
        when(documentMapper.toResponse(any(Document.class))).thenReturn(buildDocumentResponse(document));

        // When
        service.generatePresignedUrl(applicationId, uploadRequest);

        // Then
        Document savedDoc = captor.getValue();
        assertThat(savedDoc.getS3Key()).contains("null");

        verify(s3Generator).generatePresignedUrl(anyString(), anyString(), anyInt());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    @DisplayName("Should handle very large file sizes")
    void generatePresignedUrl_withLargeFileSize_shouldHandleCorrectly() {
        // Given
        long largeFileSize = 1024L * 1024 * 1024; // 1GB
        uploadRequest.setFileSize(largeFileSize);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(false);
        when(securityUtil.getCurrentUser()).thenReturn(creditOfficer);
        when(s3Generator.generatePresignedUrl(anyString(), anyString(), anyInt()))
                .thenReturn("https://s3.amazonaws.com/presigned");

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        when(documentRepository.save(captor.capture())).thenReturn(document);
        when(documentMapper.toResponse(any(Document.class))).thenReturn(buildDocumentResponse(document));

        // When
        service.generatePresignedUrl(applicationId, uploadRequest);

        // Then
        Document savedDoc = captor.getValue();
        assertThat(savedDoc.getFileSize()).isEqualTo(largeFileSize);

        verify(s3Generator).generatePresignedUrl(anyString(), anyString(), anyInt());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    @DisplayName("Should handle special characters in file name")
    void generatePresignedUrl_withSpecialCharactersInFileName_shouldHandleCorrectly() {
        // Given
        String specialFileName = "résumé_文件_2024.pdf";
        uploadRequest.setFileName(specialFileName);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(securityUtil.isApplicant()).thenReturn(false);
        when(securityUtil.getCurrentUser()).thenReturn(creditOfficer);
        when(s3Generator.generatePresignedUrl(anyString(), anyString(), anyInt()))
                .thenReturn("https://s3.amazonaws.com/presigned");

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        when(documentRepository.save(captor.capture())).thenReturn(document);
        when(documentMapper.toResponse(any(Document.class))).thenReturn(buildDocumentResponse(document));

        // When
        service.generatePresignedUrl(applicationId, uploadRequest);

        // Then
        Document savedDoc = captor.getValue();
        assertThat(savedDoc.getFileName()).isEqualTo(specialFileName);

        verify(s3Generator).generatePresignedUrl(anyString(), anyString(), anyInt());
        verify(documentRepository).save(any(Document.class));
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private DocumentResponse buildDocumentResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .applicationId(doc.getApplication().getId())
                .fileName(doc.getFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .s3PresignedUrl(doc.getS3PresignedUrl())
                .uploadedAt(doc.getUploadedAt())
                .expiresAt(doc.getExpiresAt())
                .uploadedBy(doc.getUploadedBy())
                .build();
    }

    private Document copyDocument(Document doc, String fileName, UUID newId, UUID applicationId) {
        return Document.builder()
                .id(newId)
                .application(doc.getApplication())
                .fileName(fileName)
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .s3Key("applications/" + applicationId + "/documents/" + fileName)
                .s3PresignedUrl(doc.getS3PresignedUrl())
                .uploadedAt(doc.getUploadedAt())
                .expiresAt(doc.getExpiresAt())
                .uploadedBy(doc.getUploadedBy())
                .build();
    }
}
