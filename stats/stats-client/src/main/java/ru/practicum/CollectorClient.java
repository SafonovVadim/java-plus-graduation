package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.service.collector.UserActionControllerGrpc;
import ru.practicum.service.collector.UserActionProto;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class CollectorClient {
    private final UserActionControllerGrpc.UserActionControllerBlockingStub blockingStub;

    public CompletableFuture<Void> sendUserAction(UserActionProto request) {
        return CompletableFuture.runAsync(() -> {
            try {
                blockingStub.collectUserAction(request);
                log.info("Отправлено действие пользователя в Collector");
            } catch (Exception e) {
                log.error("Ошибка отправки действия пользователя в Collector", e);
            }
        });
    }
}
