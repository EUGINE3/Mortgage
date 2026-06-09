package com.bank.notification.config;

import com.bank.notification.dto.ApplicationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-service}")
    private String groupId;

    @Value("${notification.kafka.dlq-topic:loan.applications.dlq}")
    private String dlqTopic;

    @Value("${notification.kafka.retry.initial-interval-ms:1000}")
    private long retryInitialIntervalMs;

    @Value("${notification.kafka.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${notification.kafka.retry.max-interval-ms:10000}")
    private long retryMaxIntervalMs;

    @Value("${notification.kafka.retry.max-elapsed-ms:30000}")
    private long retryMaxElapsedMs;

    @Bean
    public ConsumerFactory<String, ApplicationEvent> consumerFactory(
            @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        JsonDeserializer<ApplicationEvent> deserializer =
                new JsonDeserializer<>(ApplicationEvent.class, kafkaObjectMapper);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("com.bank.notification.dto", "com.bank.mortgage.events");

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(
            @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        JsonSerializer<Object> serializer = new JsonSerializer<>(kafkaObjectMapper);
        serializer.setAddTypeInfo(false);

        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), serializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(dlqTopic, record.partition())
        );

        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(retryInitialIntervalMs);
        backOff.setMultiplier(retryMultiplier);
        backOff.setMaxInterval(retryMaxIntervalMs);
        backOff.setMaxElapsedTime(retryMaxElapsedMs);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) ->
                org.slf4j.LoggerFactory.getLogger(KafkaConsumerConfig.class)
                        .warn("Retry attempt {} for topic={} partition={} offset={}: {}",
                                deliveryAttempt,
                                record.topic(),
                                record.partition(),
                                record.offset(),
                                exception.getMessage()));
        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ApplicationEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, ApplicationEvent> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, ApplicationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ApplicationEvent> dlqKafkaListenerContainerFactory(
            ConsumerFactory<String, ApplicationEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, ApplicationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
