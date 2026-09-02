package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProducer {
    private final KafkaProducer<String, byte[]> kafkaProducer;

    @Value("${kafka.topics.output}")
    private String similarityTopic;

    public void sendSimilarity(EventSimilarityAvro similarity) {
        byte[] avroData = serializeAvro(similarity);

        kafkaProducer.send(new ProducerRecord<>(similarityTopic,
                similarity.getEventA() + ":" + similarity.getEventB(), avroData),
                (metadata, exception) -> {
                    if (exception == null) {
                        log.info("Отправлено сходство для пары событий {} и {}: {}",
                                similarity.getEventA(), similarity.getEventB(), similarity.getScore());
                    } else {
                        log.error("Ошибка отправки сходства для пары событий {} и {}",
                                similarity.getEventA(), similarity.getEventB(), exception);
                    }
                });
    }

    private byte[] serializeAvro(EventSimilarityAvro similarity) {
        try {
            SpecificDatumWriter<EventSimilarityAvro> writer = new SpecificDatumWriter<>(EventSimilarityAvro.class);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            writer.write(similarity, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сериализации EventSimilarityAvro", e);
        }
    }
}
