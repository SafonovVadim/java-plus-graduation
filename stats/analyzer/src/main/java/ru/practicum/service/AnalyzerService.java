package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.SimilaritiesRepository;
import ru.practicum.UserActionRepository;
import ru.practicum.entity.Similarities;
import ru.practicum.entity.UserAction;
import ru.practicum.ewm.stats.proto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyzerService {
    private final SimilaritiesRepository similaritiesRepository;
    private final UserActionRepository userActionRepository;

    public Iterable<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        int userId = request.getUserId();
        int limit = request.getLimit();

        List<UserAction> userActions = userActionRepository.findAll().stream()
                .filter(action -> action.getUserId().equals((long) userId))
                .toList();

        Map<Integer, Double> eventWeights = userActions.stream()
                .collect(Collectors.toMap(
                        action -> action.getEventId().intValue(),
                        UserAction::getTotalWeight,
                        Math::max
                ));

        List<Similarities> allSimilarities = similaritiesRepository.findAll().stream().toList();

        if (eventWeights.isEmpty()) {
            return List.of();
        }

        Integer firstEventId = eventWeights.keySet().iterator().next();

        Map<Integer, List<Similarities>> eventSimilaritiesMap = allSimilarities.stream()
                .filter(sim -> sim.getEventA().equals((long) firstEventId) || sim.getEventB().equals((long) firstEventId))
                .collect(Collectors.groupingBy(sim ->
                        sim.getEventA().equals((long) firstEventId) ? sim.getEventB().intValue() : sim.getEventA().intValue()
                ));

        Map<Integer, Double> scoredEvents = eventWeights.keySet().stream()
                .flatMap(eventId -> eventSimilaritiesMap.getOrDefault(eventId, List.of()).stream()
                        .map(sim -> Map.entry(
                                sim.getEventA().equals((long) eventId) ? sim.getEventB().intValue() : sim.getEventA().intValue(),
                                sim.getScore() * eventWeights.get(eventId)
                        )))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.summingDouble(Map.Entry::getValue)
                ));

        return scoredEvents.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setPredictedRating(entry.getValue())
                        .build())
                .toList();
    }

    public Iterable<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        int eventId = request.getEventId();
        int userId = request.getUserId();
        int limit = request.getLimit();

        List<UserAction> userActions = userActionRepository.findAll().stream()
                .filter(action -> action.getUserId().equals((long) userId))
                .toList();

        Map<Integer, Double> userEventWeights = userActions.stream()
                .collect(Collectors.toMap(
                        action -> action.getEventId().intValue(),
                        UserAction::getTotalWeight,
                        Math::max
                ));

        List<Similarities> similarities = similaritiesRepository.findAll().stream()
                .filter(sim -> sim.getEventA().equals((long) eventId) || sim.getEventB().equals((long) eventId))
                .toList();

        return similarities.stream()
                .filter(sim -> !userEventWeights.containsKey(
                        sim.getEventA().equals((long) eventId) ? sim.getEventB().intValue() : sim.getEventA().intValue()
                ))
                .map(sim -> {
                    int similarEventId = sim.getEventA().equals((long) eventId) ?
                            sim.getEventB().intValue() : sim.getEventA().intValue();
                    return RecommendedEventProto.newBuilder()
                            .setEventId(similarEventId)
                            .setSimilarityCoefficient(sim.getScore())
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getSimilarityCoefficient(), a.getSimilarityCoefficient()))
                .limit(limit)
                .toList();
    }

    public Iterable<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        List<Integer> eventIds = request.getEventIdsList().stream()
                .map(Integer::intValue)
                .toList();

        List<UserAction> userActions = userActionRepository.findAll().stream()
                .filter(action -> eventIds.contains(action.getEventId().intValue()))
                .toList();

        Map<Integer, Double> eventMaxWeights = userActions.stream()
                .collect(Collectors.toMap(
                        action -> action.getEventId().intValue(),
                        UserAction::getTotalWeight,
                        Math::max
                ));

        List<RecommendedEventProto> result = new ArrayList<>();
        for (Integer eventId : eventIds) {
            Double weight = eventMaxWeights.getOrDefault(eventId, 0.0);
            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setInteractionsCount(weight.intValue())
                    .build());
        }
        return result;
    }

    public boolean hasUserViewedEvent(UserEventCheckRequestProto request) {
        return userActionRepository.existsByUserIdAndEventId(
                (long) request.getUserId(),
                (long) request.getEventId()
        );
    }
}
