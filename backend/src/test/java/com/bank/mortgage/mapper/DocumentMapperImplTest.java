package com.bank.mortgage.mapper;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.domain.Document;
import com.bank.mortgage.domain.User;
import com.bank.mortgage.dto.response.DocumentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Document Mapper Tests")
class DocumentMapperImplTest {

    private DocumentMapperImpl documentMapper;
    
    private UUID documentId;
    private UUID applicationId;
    private UUID userId;
    private Document document;
    private Instant now;
    private Instant expiresAt;

    @BeforeEach
    void setUp() {
        documentMapper = new DocumentMapperImpl();
        
        documentId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        now = Instant.now();
        expiresAt = now.plusSeconds(3600);
        
        User applicant = User.builder()
                .id(userId)
                .email("test@example.com")
                .role("APPLICANT")
                .build();
        
        Application application = Application.builder()
                .id(applicationId)
                .applicant(applicant)
                .build();
        
        document = Document.builder()
                .id(documentId)
                .application(application)
                .fileName("passport.pdf")
                .fileType("application/pdf")
                .fileSize(1024L)
                .s3Key("applications/" + applicationId + "/documents/passport.pdf")
                .s3PresignedUrl("https://s3.amazonaws.com/presigned-url")
                .uploadedAt(now)
                .expiresAt(expiresAt)
                .uploadedBy("test@example.com")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Nested
    @DisplayName("toResponse Method Tests")
    class ToResponseTests {

        @Test
        @DisplayName("Should map all fields correctly when document is valid")
        void toResponse_withValidDocument_shouldMapAllFields() {
            // When
            DocumentResponse response = documentMapper.toResponse(document);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(documentId);
            assertThat(response.getFileName()).isEqualTo("passport.pdf");
            assertThat(response.getFileType()).isEqualTo("application/pdf");
            assertThat(response.getFileSize()).isEqualTo(1024L);
            assertThat(response.getS3PresignedUrl()).isEqualTo("https://s3.amazonaws.com/presigned-url");
            assertThat(response.getUploadedAt()).isEqualTo(now);
            assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(response.getUploadedBy()).isEqualTo("test@example.com");
            assertThat(response.getCreatedAt()).isEqualTo(now);
            assertThat(response.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should return null when document is null")
        void toResponse_withNullDocument_shouldReturnNull() {
            // When
            DocumentResponse response = documentMapper.toResponse(null);

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Should handle document with null fields")
        void toResponse_withNullFields_shouldHandleGracefully() {
            // Given
            Document documentWithNulls = Document.builder()
                    .id(documentId)
                    .build();

            // When
            DocumentResponse response = documentMapper.toResponse(documentWithNulls);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(documentId);
            assertThat(response.getFileName()).isNull();
            assertThat(response.getFileType()).isNull();
            assertThat(response.getFileSize()).isNull();
            assertThat(response.getS3PresignedUrl()).isNull();
            assertThat(response.getUploadedAt()).isNull();
            assertThat(response.getExpiresAt()).isNull();
            assertThat(response.getUploadedBy()).isNull();
            assertThat(response.getCreatedAt()).isNull();
            assertThat(response.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("Should preserve field values after mapping")
        void toResponse_shouldPreserveFieldValues() {
            // When
            DocumentResponse response = documentMapper.toResponse(document);

            // Then
            assertThat(response.getId()).isEqualTo(document.getId());
            assertThat(response.getFileName()).isEqualTo(document.getFileName());
            assertThat(response.getFileType()).isEqualTo(document.getFileType());
            assertThat(response.getFileSize()).isEqualTo(document.getFileSize());
            assertThat(response.getS3PresignedUrl()).isEqualTo(document.getS3PresignedUrl());
            assertThat(response.getUploadedAt()).isEqualTo(document.getUploadedAt());
            assertThat(response.getExpiresAt()).isEqualTo(document.getExpiresAt());
            assertThat(response.getUploadedBy()).isEqualTo(document.getUploadedBy());
            assertThat(response.getCreatedAt()).isEqualTo(document.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(document.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty string values")
        void toResponse_withEmptyStrings_shouldHandleCorrectly() {
            // Given
            Document documentWithEmptyStrings = Document.builder()
                    .id(documentId)
                    .fileName("")
                    .fileType("")
                    .s3PresignedUrl("")
                    .uploadedBy("")
                    .build();

            // When
            DocumentResponse response = documentMapper.toResponse(documentWithEmptyStrings);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getFileName()).isEmpty();
            assertThat(response.getFileType()).isEmpty();
            assertThat(response.getS3PresignedUrl()).isEmpty();
            assertThat(response.getUploadedBy()).isEmpty();
        }

        @Test
        @DisplayName("Should handle very long strings")
        void toResponse_withVeryLongStrings_shouldHandleCorrectly() {
            // Given
            String veryLongFileName = "a".repeat(1000) + ".pdf";
            Document documentWithLongStrings = Document.builder()
                    .id(documentId)
                    .fileName(veryLongFileName)
                    .fileType("application/pdf")
                    .s3PresignedUrl("https://s3.amazonaws.com/" + "b".repeat(500))
                    .uploadedBy("test@" + "c".repeat(200) + ".com")
                    .build();

            // When
            DocumentResponse response = documentMapper.toResponse(documentWithLongStrings);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getFileName()).isEqualTo(veryLongFileName);
            assertThat(response.getFileName().length()).isEqualTo(1004); // 1000 'a' + ".pdf"
        }

        @Test
        @DisplayName("Should handle maximum file size")
        void toResponse_withMaxFileSize_shouldHandleCorrectly() {
            // Given
            Long maxFileSize = Long.MAX_VALUE;
            Document documentWithMaxSize = Document.builder()
                    .id(documentId)
                    .fileSize(maxFileSize)
                    .build();

            // When
            DocumentResponse response = documentMapper.toResponse(documentWithMaxSize);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getFileSize()).isEqualTo(maxFileSize);
        }

        @Test
        @DisplayName("Should handle minimum file size (zero)")
        void toResponse_withZeroFileSize_shouldHandleCorrectly() {
            // Given
            Document documentWithZeroSize = Document.builder()
                    .id(documentId)
                    .fileSize(0L)
                    .build();

            // When
            DocumentResponse response = documentMapper.toResponse(documentWithZeroSize);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getFileSize()).isZero();
        }

        @Test
        @DisplayName("Should handle past and future dates correctly")
        void toResponse_withVariousDates_shouldHandleCorrectly() {
            // Given
            Instant pastDate = Instant.parse("2020-01-01T00:00:00Z");
            Instant futureDate = Instant.parse("2030-12-31T23:59:59Z");
            
            Document documentWithDates = Document.builder()
                    .id(documentId)
                    .uploadedAt(pastDate)
                    .expiresAt(futureDate)
                    .createdAt(pastDate)
                    .updatedAt(futureDate)
                    .build();

            // When
            DocumentResponse response = documentMapper.toResponse(documentWithDates);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUploadedAt()).isEqualTo(pastDate);
            assertThat(response.getExpiresAt()).isEqualTo(futureDate);
            assertThat(response.getCreatedAt()).isEqualTo(pastDate);
            assertThat(response.getUpdatedAt()).isEqualTo(futureDate);
        }

        @Test
        @DisplayName("Should handle special characters in file name")
        void toResponse_withSpecialCharacters_shouldHandleCorrectly() {
            // Given
            String specialFileName = "résumé_文件_测试_2024!@#$%.pdf";
            Document documentWithSpecialChars = Document.builder()
                    .id(documentId)
                    .fileName(specialFileName)
                    .fileType("application/pdf")
                    .build();

            // When
            DocumentResponse response = documentMapper.toResponse(documentWithSpecialChars);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getFileName()).isEqualTo(specialFileName);
        }

        @Test
        @DisplayName("Should handle Unicode characters in uploadedBy field")
        void toResponse_withUnicodeCharacters_shouldHandleCorrectly() {
            // Given
            String unicodeEmail = "用户@例子.公司";
            Document documentWithUnicode = Document.builder()
                    .id(documentId)
                    .uploadedBy(unicodeEmail)
                    .build();

            // When
            DocumentResponse response = documentMapper.toResponse(documentWithUnicode);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUploadedBy()).isEqualTo(unicodeEmail);
        }
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create new instance each time (no caching)")
        void toResponse_shouldCreateNewInstanceEachTime() {
            // Given
            Document sameDocument = Document.builder()
                    .id(documentId)
                    .fileName("test.pdf")
                    .build();

            // When
            DocumentResponse response1 = documentMapper.toResponse(sameDocument);
            DocumentResponse response2 = documentMapper.toResponse(sameDocument);

            // Then
            assertThat(response1).isNotNull();
            assertThat(response2).isNotNull();
            assertThat(response1).isNotSameAs(response2); // Different instances
            assertThat(response1).isEqualTo(response2); // But equal in content
        }

        @Test
        @DisplayName("Should not modify original document when mapping")
        void toResponse_shouldNotModifyOriginalDocument() {
            // Given
            Document originalDocument = Document.builder()
                    .id(documentId)
                    .fileName("original.pdf")
                    .fileType("application/pdf")
                    .fileSize(1024L)
                    .build();

            // When
            DocumentResponse response = documentMapper.toResponse(originalDocument);

            // Then
            assertThat(originalDocument.getFileName()).isEqualTo("original.pdf");
            assertThat(originalDocument.getFileType()).isEqualTo("application/pdf");
            assertThat(originalDocument.getFileSize()).isEqualTo(1024L);
            
            assertThat(response.getFileName()).isEqualTo("original.pdf");
            assertThat(response.getFileType()).isEqualTo("application/pdf");
            assertThat(response.getFileSize()).isEqualTo(1024L);
        }
    }

    @Nested
    @DisplayName("Data Type Conversion Tests")
    class DataTypeConversionTests {

        @Test
        @DisplayName("Should correctly map UUID to String")
        void toResponse_shouldMapUUIDCorrectly() {
            // When
            DocumentResponse response = documentMapper.toResponse(document);

            // Then
            assertThat(response.getId()).isEqualTo(documentId);
            assertThat(response.getId().toString()).isEqualTo(documentId.toString());
        }

        @Test
        @DisplayName("Should correctly map Long file size")
        void toResponse_shouldMapLongCorrectly() {
            // Given
            Long[] testSizes = {1L, 1024L, 1048576L, 1073741824L};
            
            for (Long size : testSizes) {
                Document doc = Document.builder()
                        .id(documentId)
                        .fileSize(size)
                        .build();
                
                // When
                DocumentResponse response = documentMapper.toResponse(doc);
                
                // Then
                assertThat(response.getFileSize()).isEqualTo(size);
            }
        }

        @Test
        @DisplayName("Should correctly map Instant timestamps")
        void toResponse_shouldMapInstantCorrectly() {
            // Given
            Instant[] testInstants = {
                Instant.now(),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2025-12-31T23:59:59Z"),
                Instant.EPOCH,
                Instant.MAX,
                Instant.MIN
            };
            
            for (Instant instant : testInstants) {
                Document doc = Document.builder()
                        .id(documentId)
                        .uploadedAt(instant)
                        .expiresAt(instant)
                        .createdAt(instant)
                        .updatedAt(instant)
                        .build();
                
                // When
                DocumentResponse response = documentMapper.toResponse(doc);
                
                // Then
                assertThat(response.getUploadedAt()).isEqualTo(instant);
                assertThat(response.getExpiresAt()).isEqualTo(instant);
                assertThat(response.getCreatedAt()).isEqualTo(instant);
                assertThat(response.getUpdatedAt()).isEqualTo(instant);
            }
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle mapping of many documents without errors")
        void toResponse_withManyDocuments_shouldHandleCorrectly() {
            // Given
            int numberOfDocuments = 1000;
            
            // When & Then
            for (int i = 0; i < numberOfDocuments; i++) {
                Document doc = Document.builder()
                        .id(UUID.randomUUID())
                        .fileName("document_" + i + ".pdf")
                        .fileSize((long) i * 1024)
                        .build();
                
                DocumentResponse response = documentMapper.toResponse(doc);
                
                assertThat(response).isNotNull();
                assertThat(response.getFileName()).isEqualTo("document_" + i + ".pdf");
                assertThat(response.getFileSize()).isEqualTo((long) i * 1024);
            }
        }

        @Test
        @DisplayName("Should complete mapping quickly (timing test)")
        void toResponse_shouldCompleteQuickly() {
            // Given
            long startTime = System.nanoTime();
            
            // When
            for (int i = 0; i < 10000; i++) {
                DocumentResponse response = documentMapper.toResponse(document);
                assertThat(response).isNotNull();
            }
            
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            // Then - should complete within reasonable time (less than 1 second)
            assertThat(durationMs).isLessThan(1000);
        }
    }
}
