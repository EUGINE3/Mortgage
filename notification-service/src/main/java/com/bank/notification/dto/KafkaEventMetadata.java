package com.bank.notification.dto;

public record KafkaEventMetadata(String topic, int partition, long offset) {
}
