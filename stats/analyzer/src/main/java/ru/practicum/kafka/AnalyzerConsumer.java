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
import ru.practicum.SimilaritiesRepository;
import ru.practicum.UserActionRepository;
import ru.practicum.entity.Similarities;
import ru.practicum.entity.UserAction;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayInputStream;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerConsumer {
    private final UserActionRepository userActionRepository;
    private final SimilaritiesRepository similaritiesRepository;

    @Transactional
    @KafkaListener(topics = "${kafka.topics.input}", groupId = "analyzer-user-actions",
            containerFactory = "userActionListenerFactory")
    public void consumeUserAction(byte[] data) {
        try {
            UserActionAvro avro = deserializeAvro(data, UserActionAvro.class);
            saveUserAction(avro);
            log.info("Сохранено действие пользователя userId={} eventId={}",
                    avro.getUserId(), avro.getEventId());
        } catch (Exception e) {
            log.error("Ошибка обработки действия пользователя", e);
        }
    }

    @Transactional
    @KafkaListener(topics = "${kafka.topics.output}", groupId = "analyzer-similarity",
            containerFactory = "eventSimilarityListenerFactory")
    public void consumeSimilarity(byte[] data) {
        try {
            EventSimilarityAvro avro = deserializeAvro(data, EventSimilarityAvro.class);
            saveSimilarity(avro);

            log.info("Сохранено сходство eventA={} eventB={} score={}",
                    avro.getEventA(), avro.getEventB(), avro.getScore());
        } catch (Exception e) {
            log.error("Ошибка обработки сходства мероприятий", e);
        }
    }

    @Transactional
    public void saveUserAction(UserActionAvro avro) {
        double weight = switch (avro.getActionType()) {
            case VIEW -> 1.0;
            case REGISTER -> 2.0;
            case LIKE -> 3.0;
        };

        Optional<UserAction> existing = userActionRepository.findByUserIdAndEventId(avro.getUserId(), avro.getEventId());

        if (existing.isPresent()) {
            UserAction action = existing.get();
            if (weight > action.getTotalWeight()) {
                action.setTotalWeight(weight);
                action.setTimestamp(avro.getTimestamp());
                userActionRepository.save(action);
            }
        } else {
            UserAction action = new UserAction();
            action.setUserId(avro.getUserId());
            action.setEventId(avro.getEventId());
            action.setTotalWeight(weight);
            action.setTimestamp(avro.getTimestamp());
            userActionRepository.save(action);
        }
    }

    @Transactional
    public void saveSimilarity(EventSimilarityAvro avro) {
        Optional<Similarities> existing = similaritiesRepository.findByEventAAndEventB(avro.getEventA(), avro.getEventB());

        if (existing.isPresent()) {
            Similarities similarity = existing.get();
            similarity.setScore(avro.getScore());
            similarity.setTimestamp(avro.getTimestamp());
            similaritiesRepository.save(similarity);
        } else {
            Similarities similarity = new Similarities();
            similarity.setEventA(avro.getEventA());
            similarity.setEventB(avro.getEventB());
            similarity.setScore(avro.getScore());
            similarity.setTimestamp(avro.getTimestamp());
            similaritiesRepository.save(similarity);
        }
    }

    private <T> T deserializeAvro(byte[] data, Class<T> clazz) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
        SpecificDatumReader<T> reader = new SpecificDatumReader<>(clazz);
        return reader.read(null, decoder);
    }
}
