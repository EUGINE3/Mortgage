package com.bank.mortgage.mapper;

import com.bank.mortgage.domain.Document;
import com.bank.mortgage.dto.response.DocumentResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-11T04:05:33+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.7 (Ubuntu)"
)
@Component
public class DocumentMapperImpl implements DocumentMapper {

    @Override
    public DocumentResponse toResponse(Document document) {
        if ( document == null ) {
            return null;
        }

        DocumentResponse.DocumentResponseBuilder documentResponse = DocumentResponse.builder();

        documentResponse.createdAt( document.getCreatedAt() );
        documentResponse.expiresAt( document.getExpiresAt() );
        documentResponse.fileName( document.getFileName() );
        documentResponse.fileSize( document.getFileSize() );
        documentResponse.fileType( document.getFileType() );
        documentResponse.id( document.getId() );
        documentResponse.s3PresignedUrl( document.getS3PresignedUrl() );
        documentResponse.updatedAt( document.getUpdatedAt() );
        documentResponse.uploadedAt( document.getUploadedAt() );
        documentResponse.uploadedBy( document.getUploadedBy() );

        return documentResponse.build();
    }
}
