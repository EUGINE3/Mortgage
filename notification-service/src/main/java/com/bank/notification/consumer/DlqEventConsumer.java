package com.bank.notification.consumer;

import com.bank.notification.dto.ApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DlqEventConsumer {

    @KafkaListener(
            topics = "${notification.kafka.dlq-topic:loan.applications.dlq}",
            groupId = "${notification.kafka.dlq-group-id:notification-service-dlq}",
            containerFactory = "dlqKafkaListenerContainerFactory"
    )
    public void consumeDlq(
            @Payload ApplicationEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {

        log.error("DLQ message received topic={} partition={} offset={} applicationId={} eventType={} error={}",
                topic,
                partition,
                offset,
                event != null ? event.getApplicationId() : null,
                event != null ? event.getEventType() : null,
                exceptionMessage);
    }
}
