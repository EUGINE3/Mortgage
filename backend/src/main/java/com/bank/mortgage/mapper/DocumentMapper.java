package com.bank.mortgage.mapper;

import com.bank.mortgage.domain.Document;
import com.bank.mortgage.dto.response.DocumentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentResponse toResponse(Document document);
}