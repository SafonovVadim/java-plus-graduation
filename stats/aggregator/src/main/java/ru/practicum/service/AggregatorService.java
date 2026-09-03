package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.kafka.UserActionProducer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {
    private final UserActionProducer userActionProducer;

    private final Map<Long, Map<Long, Double>> userMaxWeights = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minSums = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventSums = new ConcurrentHashMap<>();

    private static final Map<ActionTypeAvro, Double> ACTION_WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, 1.0,
            ActionTypeAvro.REGISTER, 2.0,
            ActionTypeAvro.LIKE, 3.0
    );

    public void processUserAction(UserActionAvro userAction) {
        Long eventId = userAction.getEventId();
        Long userId = userAction.getUserId();
        double newWeight = ACTION_WEIGHTS.getOrDefault(userAction.getActionType(), 0.4);

        Map<Long, Double> eventIdWeights = userMaxWeights.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        double oldWeight = eventIdWeights.getOrDefault(eventId, 0.0);

        if (newWeight <= oldWeight) {
            log.info("Максимальный вес для пользователя {} мероприятия {} не изменился, пропускаем", userId, eventId);
            return;
        }

        double deltaSum = newWeight - oldWeight;
        eventIdWeights.put(eventId, newWeight);
        eventSums.merge(eventId, deltaSum, Double::sum);

        for (Map.Entry<Long, Double> entry : eventIdWeights.entrySet()) {
            Long otherEventId = entry.getKey();
            if (otherEventId.equals(eventId)) continue;

            double otherWeight = entry.getValue();
            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            double deltaMin = newMin - oldMin;

            addToMinSum(eventId, otherEventId, deltaMin);

            long first = Math.min(eventId, otherEventId);
            long second = Math.max(eventId, otherEventId);

            double minSum = getMinSum(eventId, otherEventId);
            double sumA = eventSums.getOrDefault(first, 0.0);
            double sumB = eventSums.getOrDefault(second, 0.0);
            double similarity = minSum / (Math.sqrt(sumA) * Math.sqrt(sumB));

            double roundedSimilarity = Math.round(similarity * 1000000.0) / 1000000.0;

            EventSimilarityAvro similarityAvro = EventSimilarityAvro.newBuilder()
                    .setEventA(Long.valueOf(first).intValue())
                    .setEventB(Long.valueOf(second).intValue())
                    .setScore(roundedSimilarity)
                    .build();

            userActionProducer.sendSimilarity(similarityAvro);
        }
    }

    private void addToMinSum(Long eventA, Long eventB, Double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);
    }

    private double getMinSum(Long eventA, Long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
    }
}
