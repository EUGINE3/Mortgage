package com.bank.mortgage.events;

import com.bank.mortgage.domain.Application;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEvent {

    private UUID applicationId;

    private String status;

    private Instant occurredAt;

    public static ApplicationEvent fromApplication(Application application) {
        return ApplicationEvent.builder()
                .applicationId(application.getId())
                .status(application.getStatus().name())
                .occurredAt(Instant.now())
                .build();
    }
}
