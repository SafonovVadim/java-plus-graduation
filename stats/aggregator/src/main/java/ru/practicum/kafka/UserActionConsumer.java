package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.AggregatorService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {
    private final AggregatorService aggregatorService;

    @KafkaListener(topics = "${kafka.topics.input}", groupId = "aggregator-group")
    public void consumeUserAction(ConsumerRecord<String, byte[]> record) {
        try {
            UserActionAvro userAction = deserializeAvro(record.value());
            aggregatorService.processUserAction(userAction);
        } catch (IOException e) {
            log.error("Ошибка десериализации UserActionAvro", e);
        }
    }

    private UserActionAvro deserializeAvro(byte[] data) throws IOException {
        org.apache.avro.specific.SpecificDatumReader<UserActionAvro> reader =
                new org.apache.avro.specific.SpecificDatumReader<>(UserActionAvro.class);
        org.apache.avro.io.Decoder decoder = org.apache.avro.io.DecoderFactory.get().binaryDecoder(data, null);
        return reader.read(null, decoder);
    }
}
