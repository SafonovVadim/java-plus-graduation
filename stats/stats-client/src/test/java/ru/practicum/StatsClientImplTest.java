package ru.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ContextConfiguration(classes = StatsClientImplTest.TestConfig.class)
@TestPropertySource(properties = "stats.service.id=stats-server")
class StatsClientImplTest {

    @Configuration
    static class TestConfig {

        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        ServiceInstanceProvider serviceInstanceProvider(ServiceInstance mockInstance) {
            return new TestServiceInstanceProvider(mockInstance);
        }

        @Bean
        ServiceInstance mockInstance() {
            return new TestServiceInstance("localhost", 8080);
        }

        @Bean
        RetryTemplate retryTemplate() {
            return new RetryTemplate();
        }
    }

    private StatsClient statsClient;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        ServiceInstanceProvider serviceInstanceProvider = new TestServiceInstanceProvider(
                new TestServiceInstance("localhost", 8080)
        );
        RetryTemplate retryTemplate = new RetryTemplate();

        this.statsClient = new StatsClientImpl(restTemplate, serviceInstanceProvider, retryTemplate);
        this.server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void shouldSendHitToServer() {
        EndpointHit hit = EndpointHit.builder()
                .app("test-app")
                .uri("/test")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2026, 4, 23, 10, 0, 0))
                .build();

        server.expect(requestTo("http://localhost:8080/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        """
                                {
                                  "app": "test-app",
                                  "uri": "/test",
                                  "ip": "192.168.0.1",
                                  "timestamp": "2026-04-23 10:00:00"
                                }"""
                ))
                .andRespond(withNoContent());

        statsClient.hit(hit);
        server.verify();
    }

    @Test
    void shouldGetStatsWithAllParameters() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 23, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 24, 0, 0, 0);
        List<String> uris = List.of("/event/1", "/event/2");
        Boolean unique = true;
        server.expect(combinedMatchers(
                pathMatches(),
                method(HttpMethod.GET),
                queryParam("start", equalTo("2026-04-23%2000:00:00")),
                queryParam("end", equalTo("2026-04-24%2000:00:00")),
                queryParam("uris", equalTo("/event/1,/event/2")),
                queryParam("unique", equalTo("true"))
        ))
                .andRespond(withSuccess(
                        """
                                [
                                  {"app": "test-app", "uri": "/event/1", "hits": 5},
                                  {"app": "test-app", "uri": "/event/2", "hits": 3}
                                ]""",
                        MediaType.APPLICATION_JSON
                ));

        List<ViewStats> result = statsClient.getStats(start, end, uris, unique);

        assertThat(result).hasSize(2);
        server.verify();
    }

    @Test
    void shouldGetStatsWithoutUrisAndUniqueFalse() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 23, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 24, 0, 0, 0);
        server.expect(combinedMatchers(
                pathMatches(),
                method(HttpMethod.GET),
                queryParam("start", equalTo("2026-04-23%2000:00:00")),
                queryParam("end", equalTo("2026-04-24%2000:00:00")),
                queryParam("unique", equalTo("false"))
        ))
                .andRespond(withSuccess(
                        """
                                [
                                  {"app": "test-app", "uri": "/home", "hits": 10}
                                ]""",
                        MediaType.APPLICATION_JSON
                ));

        List<ViewStats> result = statsClient.getStats(start, end, null, false);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getApp()).isEqualTo("test-app");
        assertThat(result.getFirst().getUri()).isEqualTo("/home");
        assertThat(result.getFirst().getHits()).isEqualTo(10L);

        server.verify();
    }

    @Test
    void shouldFormatTimestampCorrectly() {
        LocalDateTime time = LocalDateTime.of(2026, 1, 1, 12, 30, 45);
        EndpointHit hit = EndpointHit.builder()
                .app("app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(time)
                .build();

        server.expect(requestTo("http://localhost:8080/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        """
                                {
                                  "app": "app",
                                  "uri": "/test",
                                  "ip": "127.0.0.1",
                                  "timestamp": "2026-01-01 12:30:45"
                                }"""
                ))
                .andRespond(withNoContent());

        statsClient.hit(hit);

        server.verify();
    }

    private RequestMatcher combinedMatchers(RequestMatcher... matchers) {
        return request -> {
            List<String> errors = new ArrayList<>();
            for (RequestMatcher matcher : matchers) {
                try {
                    matcher.match(request);
                } catch (AssertionError e) {
                    errors.add(e.getMessage());
                }
            }
            if (!errors.isEmpty()) {
                throw new AssertionError(String.join("; ", errors));
            }
        };
    }

    private RequestMatcher pathMatches() {
        return request -> {
            String actual = request.getURI().getPath();
            if (!actual.startsWith("/stats")) {
                throw new AssertionError("Expected path to start with \"" + "/stats" + "\" but was \"" + actual + "\"");
            }
        };
    }

    /**
     * Тестовая реализация ServiceInstanceProvider, которая возвращает фиксированный ServiceInstance.
     * Не зависит от DiscoveryClient и Eureka.
     */
    private static class TestServiceInstanceProvider extends ServiceInstanceProvider {

        private final ServiceInstance fixedInstance;

        TestServiceInstanceProvider(ServiceInstance fixedInstance) {
            super(null, "");
            this.fixedInstance = fixedInstance;
        }

        @Override
        public ServiceInstance getInstance() {
            return fixedInstance;
        }
    }

    /**
     * Тестовая реализация ServiceInstance с фиксированными host и port.
     */
    private static class TestServiceInstance implements ServiceInstance {

        private final String host;
        private final int port;

        TestServiceInstance(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public String getServiceId() {
            return null;
        }

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public java.util.Map<String, String> getMetadata() {
            return java.util.Collections.emptyMap();
        }

        @Override
        public java.net.URI getUri() {
            return java.net.URI.create("http://" + host + ":" + port);
        }
    }
}
