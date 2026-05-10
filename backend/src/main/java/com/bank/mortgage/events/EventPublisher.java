package com.bank.mortgage.events;

import com.bank.mortgage.domain.Application;

public interface EventPublisher {

    void publishApplicationCreated(Application application);
}
