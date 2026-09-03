package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.SimilaritiesRepository;
import ru.practicum.UserActionRepository;
import ru.practicum.entity.Similarities;
import ru.practicum.entity.UserAction;
import ru.practicum.ewm.stats.proto.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyzerService {
    private final SimilaritiesRepository similaritiesRepository;
    private final UserActionRepository userActionRepository;

    public Iterable<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        List<UserAction> userActions = userActionRepository.findByUserId(userId);
        if (userActions.isEmpty()) {
            return List.of();
        }

        Set<Long> interactedEventIds = userActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        Map<Long, Double> userRatings = userActions.stream()
                .collect(Collectors.toMap(UserAction::getEventId, UserAction::getTotalWeight));

        List<Similarities> allSimilarities = similaritiesRepository.findAll();

        Map<Long, Double> candidateMaxSimilarity = new HashMap<>();
        for (Similarities sim : allSimilarities) {
            for (UserAction action : userActions) {
                Long candidateId = sim.getEventA().equals(action.getEventId()) ? sim.getEventB() : sim.getEventA();
                if (interactedEventIds.contains(candidateId)) continue;
                double currentMax = candidateMaxSimilarity.getOrDefault(candidateId, 0.0);
                if (sim.getScore() > currentMax) {
                    candidateMaxSimilarity.put(candidateId, sim.getScore());
                }
            }
        }

        if (candidateMaxSimilarity.isEmpty()) {
            return List.of();
        }

        List<Long> topCandidates = candidateMaxSimilarity.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();

        List<RecommendedEventProto> result = new ArrayList<>();
        for (Long candidateId : topCandidates) {
            double predictedScore = predictScore(candidateId, userRatings);
            if (predictedScore > 0) {
                result.add(RecommendedEventProto.newBuilder()
                        .setEventId(candidateId.intValue())
                        .setScore(predictedScore)
                        .build());
            }
        }
        return result;
    }

    private double predictScore(Long eventId, Map<Long, Double> userRatings) {
        List<Similarities> similarities = similaritiesRepository.findByEventId(eventId);

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (Similarities sim : similarities) {
            Long similarEventId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
            if (userRatings.containsKey(similarEventId)) {
                double rating = userRatings.get(similarEventId);
                weightedSum += sim.getScore() * rating;
                similaritySum += sim.getScore();
            }
        }

        if (similaritySum > 0) {
            return weightedSum / similaritySum;
        }
        return 0.0;
    }

    public Iterable<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        long eventId = request.getEventId();
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        List<UserAction> userActions = userActionRepository.findByUserId(userId);
        Set<Long> interactedEventIds = userActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        List<Similarities> similarities = similaritiesRepository.findByEventId(eventId);

        List<RecommendedEventProto> result = new ArrayList<>();
        for (Similarities sim : similarities) {
            Long similarId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
            if (!interactedEventIds.contains(similarId)) {
                result.add(RecommendedEventProto.newBuilder()
                        .setEventId(similarId.intValue())
                        .setScore(sim.getScore())
                        .build());
            }
        }
        result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return result.stream().limit(maxResults).collect(Collectors.toList());
    }

    public Iterable<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        List<Long> eventIds = request.getEventIdList().stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());

        List<Object[]> results = userActionRepository.sumMaxWeightByEventIdsGrouped(eventIds);
        Map<Long, Double> scoreMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));

        List<RecommendedEventProto> result = new ArrayList<>();
        for (Long eventId : eventIds) {
            Double score = scoreMap.getOrDefault(eventId, 0.0);
            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(eventId.intValue())
                    .setScore(score)
                    .build());
        }
        return result;
    }
}
