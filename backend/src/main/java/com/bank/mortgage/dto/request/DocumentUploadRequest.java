package com.bank.mortgage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File type (MIME type) is required")
    private String fileType;

    @Positive(message = "File size must be positive")
    private Long fileSize;

    private String uploadedBy;
}
