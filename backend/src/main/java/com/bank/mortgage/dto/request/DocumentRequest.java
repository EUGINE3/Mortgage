package com.bank.mortgage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentRequest {

    @NotBlank
    private String documentType;

    @NotBlank
    private String fileName;

    @NotBlank
    private String fileUrl;

    @NotBlank
    private String mimeType;

    @NotNull
    private Long fileSize;
}
