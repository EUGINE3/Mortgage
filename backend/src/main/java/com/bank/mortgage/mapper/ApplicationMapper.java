package com.bank.mortgage.mapper;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.dto.response.ApplicationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    ApplicationResponse toResponse(Application application);
}
