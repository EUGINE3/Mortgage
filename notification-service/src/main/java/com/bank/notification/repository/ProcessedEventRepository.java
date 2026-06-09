package com.bank.notification.repository;

import com.bank.notification.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByTopicAndPartitionAndOffset(String topic, int partition, long offset);
}
