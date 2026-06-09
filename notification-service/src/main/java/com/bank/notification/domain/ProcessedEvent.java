package com.bank.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_processed_events_offset",
                columnNames = {"topic", "partition_id", "offset_value"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_id", nullable = false)
    private int partition;

    @Column(name = "offset_value", nullable = false)
    private long offset;

    private UUID applicationId;

    private String eventType;

    private String correlationId;

    @Column(nullable = false)
    private Instant processedAt;
}
