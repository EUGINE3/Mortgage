package com.bank.notification.consumer;

import com.bank.notification.dto.ApplicationEvent;
import com.bank.notification.dto.KafkaEventMetadata;
import com.bank.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanApplicationEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoanApplicationEventConsumer consumer;

    @Test
    void consume_delegatesToNotificationServiceWithMetadata() {
        UUID applicationId = UUID.randomUUID();
        ApplicationEvent event = ApplicationEvent.builder()
                .eventType("CREATE")
                .applicationId(applicationId)
                .correlationId("corr-from-payload")
                .traceId("trace-from-payload")
                .build();

        consumer.consume(event, "loan.applications", 2, 99L, "app-key", null, null);

        ArgumentCaptor<KafkaEventMetadata> metadataCaptor = ArgumentCaptor.forClass(KafkaEventMetadata.class);
        verify(notificationService).handleApplicationEvent(org.mockito.ArgumentMatchers.eq(event), metadataCaptor.capture());
        KafkaEventMetadata metadata = metadataCaptor.getValue();
        assertThat(metadata.topic()).isEqualTo("loan.applications");
        assertThat(metadata.partition()).isEqualTo(2);
        assertThat(metadata.offset()).isEqualTo(99L);
    }

    @Test
    void consume_enrichesCorrelationAndTraceFromHeaders() {
        ApplicationEvent event = ApplicationEvent.builder()
                .eventType("UPDATE")
                .applicationId(UUID.randomUUID())
                .build();

        consumer.consume(event, "loan.applications", 0, 1L, null, "header-corr", "header-trace");

        assertThat(event.getCorrelationId()).isEqualTo("header-corr");
        assertThat(event.getTraceId()).isEqualTo("header-trace");
        verify(notificationService).handleApplicationEvent(event, new KafkaEventMetadata("loan.applications", 0, 1L));
    }

    @Test
    void consume_doesNotOverwriteExistingCorrelationAndTrace() {
        ApplicationEvent event = ApplicationEvent.builder()
                .eventType("UPDATE")
                .applicationId(UUID.randomUUID())
                .correlationId("payload-corr")
                .traceId("payload-trace")
                .build();

        consumer.consume(event, "loan.applications", 0, 1L, null, "header-corr", "header-trace");

        assertThat(event.getCorrelationId()).isEqualTo("payload-corr");
        assertThat(event.getTraceId()).isEqualTo("payload-trace");
    }
}
