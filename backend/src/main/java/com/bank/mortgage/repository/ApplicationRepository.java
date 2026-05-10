package com.bank.mortgage.repository;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.domain.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByApplicantId(UUID applicantId);

    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    Page<Application> findByApplicantId(UUID applicantId, Pageable pageable);

    Page<Application> findByNationalId(String nationalId, Pageable pageable);

    Page<Application> findByCreatedAtBetween(Instant startDate, Instant endDate, Pageable pageable);
}
