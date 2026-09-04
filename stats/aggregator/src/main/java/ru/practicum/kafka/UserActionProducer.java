package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    @Value("${kafka.topics.output}")
    private String TOPIC;

    public void sendSimilarity(Long eventA, Long eventB, double score) {
        try {
            EventSimilarityAvro avro = EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(score)
                    .setTimestamp(Instant.now())
                    .build();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            DatumWriter<EventSimilarityAvro> writer = new SpecificDatumWriter<>(EventSimilarityAvro.class);
            writer.write(avro, encoder);
            encoder.flush();
            byte[] bytes = out.toByteArray();

            kafkaTemplate.send(TOPIC, bytes);
            log.info("Отправлено сходство для пары событий {} и {}: {}",
                    eventA, eventB, score);
        } catch (Exception e) {
            log.error("Ошибка отправки сходства для пары событий {} и {}",
                    eventA, eventB, e);
        }
    }
}
