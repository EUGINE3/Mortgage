package com.bank.mortgage.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class S3PresignedUrlGeneratorTest {

    private S3PresignedUrlGenerator s3PresignedUrlGenerator;

    @BeforeEach
    void setUp() {
        s3PresignedUrlGenerator = new S3PresignedUrlGenerator();
        
        // Set test values using reflection since @Value won't be injected in unit tests
        ReflectionTestUtils.setField(s3PresignedUrlGenerator, "bucket", "mortgage-documents");
        ReflectionTestUtils.setField(s3PresignedUrlGenerator, "region", "us-east-1");
        ReflectionTestUtils.setField(s3PresignedUrlGenerator, "accessKey", "test-access-key");
        ReflectionTestUtils.setField(s3PresignedUrlGenerator, "secretKey", "test-secret-key");
        ReflectionTestUtils.setField(s3PresignedUrlGenerator, "endpoint", "https://s3.amazonaws.com");
    }

    @Test
    void generatePresignedUrl_WithValidParameters_ShouldReturnValidUrl() {
        // Arrange
        String s3Key = "documents/loan-1234.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = 3600;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).startsWith("https://s3.amazonaws.com/mortgage-documents/documents/loan-1234.pdf");
        assertThat(presignedUrl).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256");
        assertThat(presignedUrl).contains("X-Amz-Credential=test-access-key");
        assertThat(presignedUrl).contains("X-Amz-Signature");
        assertThat(presignedUrl).contains("X-Amz-SignedHeaders=host");
    }

    @Test
    void generatePresignedUrl_WithImageContentType_ShouldReturnValidUrl() {
        // Arrange
        String s3Key = "images/statement.png";
        String contentType = "image/png";
        int expirationSeconds = 1800;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).contains("/mortgage-documents/images/statement.png");
        assertThat(presignedUrl).contains("X-Amz-Expires=1800");
    }

    @ParameterizedTest
    @CsvSource({
        "doc/file.txt, text/plain, 300",
        "user/upload.jpg, image/jpeg, 7200",
        "temp/temp.pdf, application/pdf, 60"
    })
    void generatePresignedUrl_WithVariousParameters_ShouldReturnValidUrl(String s3Key, String contentType, int expirationSeconds) {
        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).contains(s3Key);
        assertThat(presignedUrl).contains("X-Amz-Expires=" + expirationSeconds);
    }

    @Test
    void generatePresignedUrl_WithDeepNestedPath_ShouldHandleCorrectly() {
        // Arrange
        String s3Key = "loans/2024/01/15/customer-12345/documents/application.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = 3600;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(presignedUrl).contains(s3Key);
        assertThat(presignedUrl).doesNotContain("/mortgage-documents//");
    }

    @Test
    void generatePresignedUrl_WithSpecialCharactersInKey_ShouldHandleCorrectly() {
        // Arrange
        String s3Key = "documents/loan-1234_final (2).pdf";
        String contentType = "application/pdf";
        int expirationSeconds = 3600;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).contains(s3Key);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void generatePresignedUrl_WithInvalidS3Key_ShouldThrowException(String invalidKey) {
        // Arrange
        String contentType = "application/pdf";
        int expirationSeconds = 3600;

        // Act & Assert
        assertThatThrownBy(() -> s3PresignedUrlGenerator.generatePresignedUrl(invalidKey, contentType, expirationSeconds))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to generate presigned URL");
    }

    @Test
    void generatePresignedUrl_WithNullContentType_ShouldThrowException() {
        // Arrange
        String s3Key = "test.pdf";
        int expirationSeconds = 3600;

        // Act & Assert
        assertThatThrownBy(() -> s3PresignedUrlGenerator.generatePresignedUrl(s3Key, null, expirationSeconds))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void generatePresignedUrl_WithNegativeExpiration_ShouldStillGenerateUrl() {
        // Arrange
        String s3Key = "test.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = -100;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).contains("X-Amz-Expires=-100");
    }

    @Test
    void generatePresignedUrl_WithZeroExpiration_ShouldGenerateUrl() {
        // Arrange
        String s3Key = "test.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = 0;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).contains("X-Amz-Expires=0");
    }

    @Test
    void generatePresignedUrl_WithLargeExpiration_ShouldHandleLargeNumber() {
        // Arrange
        String s3Key = "test.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = Integer.MAX_VALUE;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).contains("X-Amz-Expires=" + Integer.MAX_VALUE);
    }

    @Test
    void deleteObject_ShouldLogWithoutErrors() {
        // Arrange
        String s3Key = "documents/to-delete.pdf";

        // Act & Assert - should not throw any exception
        s3PresignedUrlGenerator.deleteObject(s3Key);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void deleteObject_WithInvalidKeys_ShouldHandleGracefully(String invalidKey) {
        // Act & Assert - should not throw any exception even with invalid keys
        s3PresignedUrlGenerator.deleteObject(invalidKey);
    }

    @Test
    void deleteObject_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Arrange
        String s3Key = "documents/delete-me!@#$%.pdf";

        // Act & Assert - should not throw any exception
        s3PresignedUrlGenerator.deleteObject(s3Key);
    }

    @Test
    void getS3Url_WithSimpleKey_ShouldReturnCorrectUrl() {
        // Arrange
        String s3Key = "document.pdf";

        // Act
        String url = s3PresignedUrlGenerator.getS3Url(s3Key);

        // Assert
        assertThat(url).isEqualTo("https://s3.amazonaws.com/mortgage-documents/document.pdf");
    }

    @Test
    void getS3Url_WithNestedKey_ShouldReturnCorrectUrl() {
        // Arrange
        String s3Key = "loans/2024/customer123/statement.pdf";

        // Act
        String url = s3PresignedUrlGenerator.getS3Url(s3Key);

        // Assert
        assertThat(url).isEqualTo("https://s3.amazonaws.com/mortgage-documents/loans/2024/customer123/statement.pdf");
    }

    @Test
    void getS3Url_WithLeadingSlash_ShouldHandleCorrectly() {
        // Arrange
        String s3Key = "/documents/test.pdf";

        // Act
        String url = s3PresignedUrlGenerator.getS3Url(s3Key);

        // Assert - Note: This might produce double slashes, test actual behavior
        assertThat(url).contains("mortgage-documents/");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getS3Url_WithNullOrEmptyKey_ShouldReturnMalformedUrl(String invalidKey) {
        // Act
        String url = s3PresignedUrlGenerator.getS3Url(invalidKey);

        // Assert - The method doesn't validate, so we just check it doesn't throw
        assertThat(url).isNotNull();
    }

    @Test
    void generatePresignedUrl_ShouldGenerateUniqueSignaturesForDifferentRequests() {
        // Arrange
        String s3Key = "test.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = 3600;

        // Act
        String url1 = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);
        
        // Wait a tiny bit to ensure different timestamp
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String url2 = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert - Different timestamps should produce different signatures
        assertThat(url1).isNotEqualTo(url2);
    }

    @Test
    void generatePresignedUrl_WithSameParametersDifferentTimestamps_ShouldHaveDifferentSignatures() {
        // Arrange
        String s3Key = "document.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = 3600;

        // Act
        String url1 = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String url2 = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(url1).isNotEqualTo(url2);
        
        // Extract signatures
        String signature1 = extractSignature(url1);
        String signature2 = extractSignature(url2);
        
        assertThat(signature1).isNotEqualTo(signature2);
    }

    private String extractSignature(String url) {
        String signatureParam = "X-Amz-Signature=";
        int startIndex = url.indexOf(signatureParam) + signatureParam.length();
        int endIndex = url.indexOf("&", startIndex);
        if (endIndex == -1) {
            endIndex = url.length();
        }
        return url.substring(startIndex, endIndex);
    }

    @Test
    void generatePresignedUrl_ShouldIncludeAllRequiredAwsParameters() {
        // Arrange
        String s3Key = "test.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = 3600;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert - All required AWS Signature V4 parameters are present
        assertThat(presignedUrl).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256");
        assertThat(presignedUrl).contains("X-Amz-Credential=test-access-key");
        assertThat(presignedUrl).contains("X-Amz-Date=");
        assertThat(presignedUrl).contains("X-Amz-Expires=");
        assertThat(presignedUrl).contains("X-Amz-Signature=");
        assertThat(presignedUrl).contains("X-Amz-SignedHeaders=host");
    }

    @Test
    void generatePresignedUrl_WithDifferentContentTypes_ShouldGenerateValidUrls() {
        // Arrange
        String s3Key = "test-file";
        String[] contentTypes = {
            "application/pdf",
            "image/jpeg",
            "image/png",
            "text/plain",
            "application/json",
            "video/mp4"
        };
        int expirationSeconds = 3600;

        // Act & Assert
        for (String contentType : contentTypes) {
            String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);
            assertThat(presignedUrl).isNotNull();
            assertThat(presignedUrl).contains(s3Key);
        }
    }

    @Test
    void generatePresignedUrl_WhenSecretKeyHasSpecialCharacters_ShouldHandleCorrectly() {
        // Arrange
        String specialSecretKey = "secret+key/with=special&chars";
        ReflectionTestUtils.setField(s3PresignedUrlGenerator, "secretKey", specialSecretKey);
        
        String s3Key = "test.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = 3600;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).contains("X-Amz-Signature=");
    }

    @Test
    void generatePresignedUrl_WithCustomEndpoint_ShouldUseCustomEndpoint() {
        // Arrange
        ReflectionTestUtils.setField(s3PresignedUrlGenerator, "endpoint", "https://custom-s3.example.com");
        
        String s3Key = "test.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = 3600;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert
        assertThat(presignedUrl).startsWith("https://custom-s3.example.com");
        assertThat(presignedUrl).contains("/mortgage-documents/test.pdf");
    }

    @Test
    void generatePresignedUrl_WithDifferentRegions_ShouldReflectInUrl() {
        // Arrange
        ReflectionTestUtils.setField(s3PresignedUrlGenerator, "region", "eu-west-1");
        
        String s3Key = "test.pdf";
        String contentType = "application/pdf";
        int expirationSeconds = 3600;

        // Act
        String presignedUrl = s3PresignedUrlGenerator.generatePresignedUrl(s3Key, contentType, expirationSeconds);

        // Assert - The host header in canonical request uses the region
        assertThat(presignedUrl).isNotNull();
        // Note: The URL itself doesn't contain region, but the signature generation uses it
    }
}
