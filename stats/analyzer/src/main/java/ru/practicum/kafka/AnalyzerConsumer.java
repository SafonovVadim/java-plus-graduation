package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.SimilaritiesRepository;
import ru.practicum.UserActionRepository;
import ru.practicum.entity.Similarities;
import ru.practicum.entity.UserAction;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerConsumer {
    private final UserActionRepository userActionRepository;
    private final SimilaritiesRepository similaritiesRepository;

    @KafkaListener(topics = "${kafka.topics.input}", groupId = "analyzer-group")
    public void consumeUserAction(ConsumerRecord<String, byte[]> record) {
        CompletableFuture.runAsync(() -> {
            try {
                UserActionAvro avro = deserializeAvro(record.value(), UserActionAvro.class);
                saveUserAction(avro);
                log.info("Сохранено действие пользователя userId={} eventId={}",
                        avro.getUserId(), avro.getEventId());
            } catch (Exception e) {
                log.error("Ошибка обработки действия пользователя", e);
            }
        });
    }

    @KafkaListener(topics = "${kafka.topics.similarity}", groupId = "analyzer-group")
    public void consumeSimilarity(ConsumerRecord<String, byte[]> record) {
        CompletableFuture.runAsync(() -> {
            try {
                EventSimilarityAvro avro = deserializeAvro(record.value(), EventSimilarityAvro.class);
                saveSimilarity(avro);
                log.info("Сохранено сходство eventA={} eventB={} score={}",
                        avro.getEventA(), avro.getEventB(), avro.getScore());
            } catch (Exception e) {
                log.error("Ошибка обработки сходства мероприятий", e);
            }
        });
    }

    private void saveUserAction(UserActionAvro avro) {
        userActionRepository.findAll().stream()
                .filter(action -> action.getUserId().equals(avro.getUserId()) &&
                        action.getEventId().equals(avro.getEventId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {
                            if (avro.getActionType().ordinal() > existing.getMaxWeight().intValue()) {
                                existing.setMaxWeight(avro.getActionType().ordinal() + 1.0);
                                existing.setTimestamp(new Timestamp(avro.getTimestamp()));
                                userActionRepository.save(existing);
                            }
                        },
                        () -> {
                            UserAction action = new UserAction();
                            action.setUserId(avro.getUserId());
                            action.setEventId(avro.getEventId());
                            action.setMaxWeight(avro.getActionType().ordinal() + 1.0);
                            action.setTimestamp(new Timestamp(avro.getTimestamp()));
                            userActionRepository.save(action);
                        }
                );
    }

    private void saveSimilarity(EventSimilarityAvro avro) {
        similaritiesRepository.findAll().stream()
                .filter(sim -> (sim.getEventA().equals(avro.getEventA()) &&
                        sim.getEventB().equals(avro.getEventB())) ||
                        (sim.getEventA().equals(avro.getEventB()) &&
                                sim.getEventB().equals(avro.getEventA())))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {
                            existing.setScore(avro.getScore());
                            existing.setTimestamp(new Timestamp(avro.getTimestamp()));
                            similaritiesRepository.save(existing);
                        },
                        () -> {
                            Similarities similarity = new Similarities();
                            similarity.setEventA(avro.getEventA());
                            similarity.setEventB(avro.getEventB());
                            similarity.setScore(avro.getScore());
                            similarity.setTimestamp(new Timestamp(avro.getTimestamp()));
                            similaritiesRepository.save(similarity);
                        }
                );
    }

    private <T> T deserializeAvro(byte[] data, Class<T> clazz) throws Exception {
        SpecificDatumReader<T> reader = new SpecificDatumReader<>(clazz);
        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        return reader.read(null, decoder);
    }
}
