package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.AggregatorService;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final AggregatorService aggregatorService;

    @KafkaListener(topics = "${kafka.topics.input}", groupId = "aggregator")
    public void consumeUserAction(ConsumerRecord<String, byte[]> record) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(record.value());
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
            SpecificDatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.class);
            UserActionAvro action = reader.read(null, decoder);

            log.info("Получено действие: userId={}, eventId={}, type={}",
                    action.getUserId(), action.getEventId(), action.getActionType());

            aggregatorService.processUserAction(
                    action.getEventId(),
                    action.getUserId(),
                    action.getActionType().name()
            );
        } catch (Exception e) {
            log.error("Ошибка десериализации UserActionAvro", e);
        }
    }
}
