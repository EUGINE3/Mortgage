package com.bank.notification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEvent {

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("application_id")
    private UUID applicationId;

    @JsonProperty("applicant_id")
    private UUID applicantId;

    @JsonProperty("applicant_email")
    private String applicantEmail;

    @JsonProperty("applicant_name")
    private String applicantName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("loan_amount")
    @JsonAlias("loanAmount")
    private BigDecimal loanAmount;

    @JsonProperty("national_id")
    @JsonAlias("nationalId")
    private String nationalId;

    @JsonProperty("created_at")
    @JsonAlias("createdAt")
    private Instant createdAt;

    @JsonProperty("updated_at")
    @JsonAlias("updatedAt")
    private Instant updatedAt;

    @JsonProperty("correlation_id")
    @JsonAlias("correlationId")
    private String correlationId;

    @JsonProperty("trace_id")
    @JsonAlias("traceId")
    private String traceId;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("version")
    private String version;
}
