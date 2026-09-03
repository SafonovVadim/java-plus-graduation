package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.kafka.UserActionProducer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {
    private final UserActionProducer userActionProducer;

    private final Map<Integer, Map<Integer, Double>> eventUserWeights = new ConcurrentHashMap<>();
    private final Map<Integer, Double> eventSumSquareWeights = new ConcurrentHashMap<>();

    public void processUserAction(UserActionAvro userAction) {
        int eventId = Long.valueOf(userAction.getEventId()).intValue();
        int userId = Long.valueOf(userAction.getUserId()).intValue();
        Instant timestamp = userAction.getTimestamp();
        double weight = getActionWeight(userAction.getActionType());

        eventUserWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        Map<Integer, Double> userIdWeights = eventUserWeights.get(eventId);

        double oldWeight = userIdWeights.getOrDefault(userId, 1.0);
        if (weight <= oldWeight) {
            log.info("Максимальный вес для пользователя {} мероприятия {} не изменился, пропускаем", userId, eventId);
            return;
        }

        double weightDifference = weight - oldWeight;
        userIdWeights.put(userId, weight);

        eventSumSquareWeights.merge(eventId,
                weightDifference * (oldWeight + weight),
                Double::sum);

        List<Integer> otherEventIds = eventUserWeights.keySet().stream()
                .filter(id -> !id.equals(eventId))
                .filter(otherEventId -> hasCommonUsers(eventId, otherEventId))
                .toList();

        for (int otherEventId : otherEventIds) {
            double similarity = calculateCosineSimilarity(eventId, otherEventId);
            sendSimilarity(eventId, otherEventId, similarity, timestamp);
        }
    }

    private boolean hasCommonUsers(int eventA, int eventB) {
        Map<Integer, Double> usersA = eventUserWeights.get(eventA);
        Map<Integer, Double> usersB = eventUserWeights.get(eventB);
        if (usersA == null || usersB == null) {
            return false;
        }
        return usersA.keySet().stream().anyMatch(usersB::containsKey);
    }

    private double getMinWeightSum(int eventA, int eventB) {
        Map<Integer, Double> usersA = eventUserWeights.get(eventA);
        Map<Integer, Double> usersB = eventUserWeights.get(eventB);
        if (usersA == null || usersB == null) {
            return 0.0;
        }
        return usersA.keySet().stream()
                .filter(usersB::containsKey)
                .mapToDouble(userId -> Math.min(usersA.get(userId), usersB.get(userId)))
                .sum();
    }

    private double getActionWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 1.0;
            case REGISTER -> 2.0;
            case LIKE -> 3.0;
        };
    }

    private double calculateCosineSimilarity(int eventA, int eventB) {
        double sumMin = getMinWeightSum(eventA, eventB);
        double sumSquareA = eventSumSquareWeights.getOrDefault(eventA, 0.0);
        double sumSquareB = eventSumSquareWeights.getOrDefault(eventB, 0.0);

        if (sumSquareA == 0 || sumSquareB == 0) {
            return 0.0;
        }
        return sumMin / (Math.sqrt(sumSquareA) * Math.sqrt(sumSquareB));
    }

    private void sendSimilarity(int eventA, int eventB, double score, Instant timestamp) {
        int firstEvent = Math.min(eventA, eventB);
        int secondEvent = Math.max(eventA, eventB);

        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(firstEvent)
                .setEventB(secondEvent)
                .setScore(score)
                .setTimestamp(timestamp)
                .build();

        userActionProducer.sendSimilarity(similarity);
    }
}
