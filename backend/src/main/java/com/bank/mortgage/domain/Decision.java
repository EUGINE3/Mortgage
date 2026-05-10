package com.bank.mortgage.domain;

import com.bank.mortgage.domain.enums.DecisionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "decisions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Decision {

    @Id
    private UUID id;

    @Column(name = "application_id")
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    private DecisionType decisionType;

    private String reason;

    private String decidedBy;

    private Instant decidedAt;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Integer version;
}
