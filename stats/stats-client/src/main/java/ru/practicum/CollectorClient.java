package ru.practicum;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;


import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
public class CollectorClient {
    private final UserActionControllerGrpc.UserActionControllerBlockingStub blockingStub;

    public CollectorClient(UserActionControllerGrpc.UserActionControllerBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

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
