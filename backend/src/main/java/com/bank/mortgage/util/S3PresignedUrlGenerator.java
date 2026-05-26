package com.bank.mortgage.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
@Slf4j
public class S3PresignedUrlGenerator {

    @Value("${aws.s3.bucket:mortgage-documents}")
    private String bucket;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.access-key:mock-access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key:mock-secret-key}")
    private String secretKey;

    @Value("${aws.s3.endpoint:https://s3.amazonaws.com}")
    private String endpoint;

    /**
     * Generates an S3-style presigned URL for uploading documents
     * In a production environment, this would use AWS SDK (e.g., S3Presigner)
     * For now, returns a mock presigned URL with proper structure
     */
    public String generatePresignedUrl(String s3Key, String contentType, int expirationSeconds) {
        try {
            long now = System.currentTimeMillis() / 1000;
            long expiration = now + expirationSeconds;

            // Build the canonical request for AWS Signature Version 4
            String canonicalRequest = buildCanonicalRequest(s3Key, contentType, expiration);
            
            // Sign the request
            String signature = signRequest(canonicalRequest);

            // Build presigned URL
            String presignedUrl = buildPresignedUrl(s3Key, signature, now, expiration);
            
            log.debug("Generated presigned URL for S3 key: {}", s3Key);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Error generating presigned URL for S3 key: {}", s3Key, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Delete object from S3 (mock implementation)
     */
    public void deleteObject(String s3Key) {
        log.info("Delete request for S3 key: {} (mock implementation)", s3Key);
        // In production, this would use AWS SDK to delete from S3
    }

    private String buildCanonicalRequest(String s3Key, String contentType, long expiration) {
        return String.format(
            "PUT\n/%s/%s\n\nhost:%s.s3.%s.amazonaws.com\n" +
            "x-amz-content-sha256:UNSIGNED-PAYLOAD\nx-amz-date:%d\n\n" +
            "host;x-amz-content-sha256;x-amz-date\nUNSIGNED-PAYLOAD",
            bucket, s3Key, bucket, region, expiration
        );
    }

    private String signRequest(String canonicalRequest) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signature = mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }

    private String buildPresignedUrl(String s3Key, String signature, long now, long expiration) {
        return String.format(
            "%s/%s/%s?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=%s&" +
            "X-Amz-Date=%d&X-Amz-Expires=%d&X-Amz-Signature=%s&X-Amz-SignedHeaders=host",
            endpoint, bucket, s3Key, accessKey, now, (expiration - now), signature
        );
    }

    public String getS3Url(String s3Key) {
        return String.format("%s/%s/%s", endpoint, bucket, s3Key);
    }
}
