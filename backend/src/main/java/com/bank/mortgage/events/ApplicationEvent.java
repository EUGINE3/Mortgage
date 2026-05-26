package com.bank.mortgage.events;

import com.bank.mortgage.domain.Application;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("event_type")
    private String eventType; // CREATE, UPDATE, DELETE

    @JsonProperty("application_id")
    private UUID applicationId;

    @JsonProperty("applicant_id")
    private UUID applicantId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("loan_amount")
    private java.math.BigDecimal loanAmount;

    @JsonProperty("national_id")
    private String nationalId;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("version")
    private String version;

    public static ApplicationEvent fromApplication(Application app, String eventType, String correlationId, String traceId) {
        return ApplicationEvent.builder()
                .eventType(eventType)
                .applicationId(app.getId())
                .applicantId(app.getApplicant().getId())
                .status(app.getStatus().name())
                .loanAmount(app.getLoanAmount())
                .nationalId(app.getNationalId())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .correlationId(correlationId)
                .traceId(traceId)
                .timestamp(Instant.now())
                .version("1.0")
                .build();
    }
}
