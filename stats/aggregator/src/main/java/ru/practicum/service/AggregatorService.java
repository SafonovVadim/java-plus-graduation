package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.kafka.UserActionProducer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorService {
    private final UserActionProducer userActionProducer;

    private final Map<Long, Map<Long, Double>> userMaxWeights = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minSums = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventSums = new ConcurrentHashMap<>();

    private static final Map<String, Double> ACTION_WEIGHTS = Map.of(
            "VIEW", 0.4,
            "REGISTER", 0.8,
            "LIKE", 1.0
    );

    public void processUserAction(Long eventId, Long userId, String actionType) {
        double newWeight = ACTION_WEIGHTS.getOrDefault(actionType, 0.4);
        double oldWeight = getUserWeightForEvent(userId, eventId);

        log.info("Обработка: userId={}, eventId={}, type={}, weight={}",
                userId, eventId, actionType, newWeight);
        if (newWeight <= oldWeight) {
            log.info("Пропуск: newWeight={} <= oldWeight={}", newWeight, oldWeight);
            return;
        }

        putUserWeight(userId, eventId, newWeight);
        double deltaSum = newWeight - oldWeight;
        addToEventSum(eventId, deltaSum);
        Map<Long, Double> userEvents = getUserWeights(userId);

        for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {
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
            double sumA = getEventSum(first);
            double sumB = getEventSum(second);
            double similarity = minSum / (Math.sqrt(sumA) * Math.sqrt(sumB));

            double roundedSimilarity = Math.round(similarity * 1000000.0) / 1000000.0;

            log.info("Обновление пары ({},{}): deltaMin={}, similarity={}", first, second, deltaMin, roundedSimilarity);

            userActionProducer.sendSimilarity(first, second, roundedSimilarity);
        }
    }

    private Map<Long, Double> getUserWeights(Long userId) {
        return userMaxWeights.getOrDefault(userId, Map.of());
    }

    private Double getUserWeightForEvent(Long userId, Long eventId) {
        return userMaxWeights.getOrDefault(userId, Map.of()).getOrDefault(eventId, 0.0);
    }

    private void putUserWeight(Long userId, Long eventId, Double weight) {
        userMaxWeights.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(eventId, weight);
    }

    private void addToEventSum(Long eventId, Double delta) {
        eventSums.merge(eventId, delta, Double::sum);
    }

    private Double getEventSum(Long eventId) {
        return eventSums.getOrDefault(eventId, 0.0);
    }

    public void addToMinSum(Long eventA, Long eventB, Double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);
    }

    public Double getMinSum(Long eventA, Long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
    }
}
