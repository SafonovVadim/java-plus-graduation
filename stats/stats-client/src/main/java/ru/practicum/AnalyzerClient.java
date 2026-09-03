package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.*;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@Service
public class AnalyzerClient {

    private final RecommendationsControllerGrpc.RecommendationsControllerBlockingStub blockingStub;

    public AnalyzerClient(RecommendationsControllerGrpc.RecommendationsControllerBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    public Stream<RecommendedEventProto> getRecommendationsForUser(int userId, int limit) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setLimit(limit)
                .build();

        Iterator<RecommendedEventProto> iterator = blockingStub.getRecommendationsForUser(request);
        return asStream(iterator);
    }

    public Stream<RecommendedEventProto> getSimilarEvents(int eventId, int userId, int limit) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setLimit(limit)
                .build();

        Iterator<RecommendedEventProto> iterator = blockingStub.getSimilarEvents(request);
        return asStream(iterator);
    }

    public Stream<RecommendedEventProto> getInteractionsCount(int[] eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventIds(java.util.Arrays.stream(eventIds).boxed().collect(java.util.stream.Collectors.toList()))
                .build();

        Iterator<RecommendedEventProto> iterator = blockingStub.getInteractionsCount(request);
        return asStream(iterator);
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
