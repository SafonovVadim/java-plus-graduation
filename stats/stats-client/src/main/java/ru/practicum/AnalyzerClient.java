package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.service.dashboard.*;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class AnalyzerClient {
    @GrpcClient("analyzer")
    private final RecommendationsControllerGrpc.RecommendationsControllerBlockingStub blockingStub;

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

    public boolean hasUserViewedEvent(int userId, int eventId) {
        UserEventCheckRequestProto request = UserEventCheckRequestProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .build();

        CheckEventResponseProto response = blockingStub.hasUserViewedEvent(request);
        return response.getHasViewed();
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
