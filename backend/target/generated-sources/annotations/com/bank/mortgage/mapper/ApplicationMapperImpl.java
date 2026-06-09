package com.bank.mortgage.mapper;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.dto.response.ApplicationResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-04T12:44:02+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Ubuntu)"
)
@Component
public class ApplicationMapperImpl implements ApplicationMapper {

    @Override
    public ApplicationResponse toResponse(Application application) {
        if ( application == null ) {
            return null;
        }

        ApplicationResponse.ApplicationResponseBuilder applicationResponse = ApplicationResponse.builder();

        applicationResponse.id( application.getId() );
        if ( application.getStatus() != null ) {
            applicationResponse.status( application.getStatus().name() );
        }
        applicationResponse.loanAmount( application.getLoanAmount() );
        applicationResponse.tenureMonths( application.getTenureMonths() );
        applicationResponse.createdAt( application.getCreatedAt() );

        return applicationResponse.build();
    }
}
