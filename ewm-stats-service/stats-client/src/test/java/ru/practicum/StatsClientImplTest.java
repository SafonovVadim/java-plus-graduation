package ru.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ContextConfiguration(classes = StatsClientImplTest.TestConfig.class)
@TestPropertySource(properties = "stats.server.url=http://localhost:8080")
class StatsClientImplTest {

    @Configuration
    static class TestConfig {

    }

    private StatsClient statsClient;

    private MockRestServiceServer server;

    private static final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        this.statsClient = new StatsClientImpl(restTemplate, BASE_URL);
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

        server.expect(requestTo(BASE_URL + "/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        "{\n" +
                                "  \"app\": \"test-app\",\n" +
                                "  \"uri\": \"/test\",\n" +
                                "  \"ip\": \"192.168.0.1\",\n" +
                                "  \"timestamp\": \"2026-04-23 10:00:00\"\n" +
                                "}"
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
        server.expect(anyRequestToPath(BASE_URL + "/stats"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("start", equalTo("2026-04-23%2000:00:00")))
                .andExpect(queryParam("end", equalTo("2026-04-24%2000:00:00")))
                .andExpect(queryParam("uris", equalTo(String.join(",", uris))))
                .andExpect(queryParam("unique", equalTo(unique.toString())))
                .andRespond(withSuccess(
                        "[\n" +
                                "  {\"app\": \"test-app\", \"uri\": \"/event/1\", \"hits\": 5},\n" +
                                "  {\"app\": \"test-app\", \"uri\": \"/event/2\", \"hits\": 3}\n" +
                                "]",
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
        server.expect(anyRequestToPath(BASE_URL + "/stats"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("start", equalTo("2026-04-23%2000:00:00")))
                .andExpect(queryParam("end", equalTo("2026-04-24%2000:00:00")))
                .andExpect(queryParam("unique", equalTo("false")))
                .andRespond(withSuccess(
                        "[\n" +
                                "  {\"app\": \"test-app\", \"uri\": \"/home\", \"hits\": 10}\n" +
                                "]",
                        MediaType.APPLICATION_JSON
                ));

        List<ViewStats> result = statsClient.getStats(start, end, null, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApp()).isEqualTo("test-app");
        assertThat(result.get(0).getUri()).isEqualTo("/home");
        assertThat(result.get(0).getHits()).isEqualTo(10L);

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

        server.expect(requestTo(BASE_URL + "/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        "{\n" +
                                "  \"app\": \"app\",\n" +
                                "  \"uri\": \"/test\",\n" +
                                "  \"ip\": \"127.0.0.1\",\n" +
                                "  \"timestamp\": \"2026-01-01 12:30:45\"\n" +
                                "}"
                ))
                .andRespond(withNoContent());

        statsClient.hit(hit);

        server.verify();
    }

    private RequestMatcher anyRequestToPath(String path) {
        return request -> {
            String actual = request.getURI().toString();
            if (!actual.startsWith(path)) {
                throw new AssertionError("Expected URL to start with \"" + path + "\" but was \"" + actual + "\"");
            }
        };
    }
}
