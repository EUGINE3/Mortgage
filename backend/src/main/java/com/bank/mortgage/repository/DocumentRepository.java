package com.bank.mortgage.repository;

import com.bank.mortgage.domain.Application;
import com.bank.mortgage.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByApplication(Application application, Pageable pageable);
}
