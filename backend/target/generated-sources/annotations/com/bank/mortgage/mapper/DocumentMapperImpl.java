package com.bank.mortgage.mapper;

import com.bank.mortgage.domain.Document;
import com.bank.mortgage.dto.response.DocumentResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-23T12:03:20+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Ubuntu)"
)
@Component
public class DocumentMapperImpl implements DocumentMapper {

    @Override
    public DocumentResponse toResponse(Document document) {
        if ( document == null ) {
            return null;
        }

        DocumentResponse.DocumentResponseBuilder documentResponse = DocumentResponse.builder();

        documentResponse.id( document.getId() );
        documentResponse.fileName( document.getFileName() );
        documentResponse.fileType( document.getFileType() );
        documentResponse.fileSize( document.getFileSize() );
        documentResponse.s3PresignedUrl( document.getS3PresignedUrl() );
        documentResponse.uploadedAt( document.getUploadedAt() );
        documentResponse.expiresAt( document.getExpiresAt() );
        documentResponse.uploadedBy( document.getUploadedBy() );
        documentResponse.createdAt( document.getCreatedAt() );
        documentResponse.updatedAt( document.getUpdatedAt() );

        return documentResponse.build();
    }
}
