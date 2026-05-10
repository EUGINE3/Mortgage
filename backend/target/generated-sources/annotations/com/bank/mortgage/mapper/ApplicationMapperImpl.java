package com.bank.mortgage.mapper;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.dto.response.ApplicationResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-10T16:35:41+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.7 (Ubuntu)"
)
@Component
public class ApplicationMapperImpl implements ApplicationMapper {

    @Override
    public ApplicationResponse toResponse(Application application) {
        if ( application == null ) {
            return null;
        }

        ApplicationResponse.ApplicationResponseBuilder applicationResponse = ApplicationResponse.builder();

        applicationResponse.createdAt( application.getCreatedAt() );
        applicationResponse.id( application.getId() );
        applicationResponse.loanAmount( application.getLoanAmount() );
        if ( application.getStatus() != null ) {
            applicationResponse.status( application.getStatus().name() );
        }
        applicationResponse.tenureMonths( application.getTenureMonths() );

        return applicationResponse.build();
    }
}
