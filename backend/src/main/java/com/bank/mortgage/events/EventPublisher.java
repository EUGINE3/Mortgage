package com.bank.mortgage.events;

import com.bank.mortgage.domain.Application;

public interface EventPublisher {

    void publishApplicationCreated(Application application);

    void publishApplicationUpdated(Application application);

    void publishApplicationDeleted(String applicationId);
}
