package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.AggregatorService;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {
    private final AggregatorService aggregatorService;

    @KafkaListener(topics = "${kafka.topics.input}", groupId = "aggregator-user-actions",
            containerFactory = "userActionListenerFactory")
    public void consumeUserAction(ConsumerRecord<String, byte[]> record) {
        try {
            UserActionAvro userAction = deserializeAvro(record.value());
            aggregatorService.processUserAction(userAction);
        } catch (IOException e) {
            log.error("Ошибка десериализации UserActionAvro", e);
        }
    }

    private UserActionAvro deserializeAvro(byte[] data) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
        SpecificDatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.class);
        return reader.read(null, decoder);
    }
}
