package ru.practicum.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.auto-offset-reset:latest}")
    private String autoOffsetReset;

    @Bean
    public ConsumerFactory<String, byte[]> userActionConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory(
            ConsumerFactory<String, byte[]> userActionConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userActionConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new DefaultErrorHandler());
        return factory;
    }

    @Bean
    public ApplicationRunner seekToEndUserActions(ConsumerFactory<String, byte[]> userActionConsumerFactory) {
        return args -> {
            try (var consumer = userActionConsumerFactory.createConsumer("analyzer-reset-ua", "reset")) {
                var partitions = consumer.partitionsFor("stats.user-actions.v1")
                        .stream()
                        .map(p -> new TopicPartition(p.topic(), p.partition()))
                        .collect(Collectors.toList());
                if (!partitions.isEmpty()) {
                    consumer.assign(partitions);
                    consumer.seekToEnd(partitions);
                    consumer.commitSync();
                    log.info("Analyzer user-actions offset reset to end");
                }
            }
        };
    }

    @Bean
    public ApplicationRunner seekToEndEventSimilarity(ConsumerFactory<String, byte[]> userActionConsumerFactory) {
        return args -> {
            try (var consumer = userActionConsumerFactory.createConsumer("analyzer-reset-es", "reset")) {
                var partitions = consumer.partitionsFor("stats.events-similarity.v1")
                        .stream()
                        .map(p -> new TopicPartition(p.topic(), p.partition()))
                        .collect(Collectors.toList());
                if (!partitions.isEmpty()) {
                    consumer.assign(partitions);
                    consumer.seekToEnd(partitions);
                    consumer.commitSync();
                    log.info("Analyzer event-similarity offset reset to end");
                }
            }
        };
    }
}
