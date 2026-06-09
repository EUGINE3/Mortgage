package com.bank.notification.service;

import com.bank.notification.dto.ApplicationEvent;
import com.bank.notification.dto.KafkaEventMetadata;

public interface NotificationService {

    void handleApplicationEvent(ApplicationEvent event, KafkaEventMetadata metadata);
}
