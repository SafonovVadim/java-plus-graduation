package ru.practicum;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;

@Configuration
public class GrpcClientConfig {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    @Bean
    public AnalyzerClient analyzerClient() {
        return new AnalyzerClient(analyzerStub);
    }

    @Bean
    public CollectorClient collectorClient() {
        return new CollectorClient(collectorStub);
    }
}
