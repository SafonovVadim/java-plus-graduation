package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.entity.EndpointHitEntity;
import ru.practicum.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DataJpaTest
@Import(StatsService.class)
public class StatsServiceAppTest {

    @Autowired
    private StatsRepository statsRepository;

    @Autowired
    private StatsService statsService;

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 4, 23, 10, 0, 0);

    @Test
    void shouldSaveHit() {
        EndpointHit hit = EndpointHit.builder()
                .app("test-app")
                .uri("/test")
                .ip("192.168.0.1")
                .timestamp(BASE_TIME)
                .build();
        statsService.saveHit(hit);

        List<EndpointHitEntity> saved = statsRepository.findAll();
        assertThat(saved, hasSize(1));
        assertThat(saved.get(0).getApp(), equalTo("test-app"));
        assertThat(saved.get(0).getUri(), equalTo("/test"));
        assertThat(saved.get(0).getIp(), equalTo("192.168.0.1"));
        assertThat(saved.get(0).getTimestamp(), equalTo(BASE_TIME));
    }

    @Test
    void shouldReturnAllHitsWhenUniqueFalse() {
        saveHit("app1", "/event/1", "192.168.0.1", BASE_TIME);
        saveHit("app1", "/event/1", "192.168.0.1", BASE_TIME.plusSeconds(1));
        saveHit("app1", "/event/1", "192.168.0.2", BASE_TIME.plusSeconds(2));
        List<ViewStats> stats = statsService.getStats(
                BASE_TIME.minusHours(1),
                BASE_TIME.plusHours(1),
                null,
                false
        );
        assertThat(stats, hasSize(1));
        ViewStats result = stats.get(0);
        assertThat(result.getApp(), equalTo("app1"));
        assertThat(result.getUri(), equalTo("/event/1"));
        assertThat(result.getHits(), equalTo(3L));
    }

    @Test
    void shouldReturnUniqueHitsWhenUniqueTrue() {
        saveHit("app1", "/event/1", "192.168.0.1", BASE_TIME);
        saveHit("app1", "/event/1", "192.168.0.1", BASE_TIME.plusSeconds(1));
        saveHit("app1", "/event/1", "192.168.0.2", BASE_TIME.plusSeconds(2));
        List<ViewStats> stats = statsService.getStats(
                BASE_TIME.minusHours(1),
                BASE_TIME.plusHours(1),
                null,
                true
        );
        assertThat(stats, hasSize(1));
        ViewStats result = stats.get(0);
        assertThat(result.getHits(), equalTo(2L)); // только 2 уникальных IP
    }

    @Test
    void shouldFilterByUris() {
        saveHit("app1", "/event/1", "192.168.0.1", BASE_TIME);
        saveHit("app1", "/event/2", "192.168.0.2", BASE_TIME);
        saveHit("app1", "/other", "192.168.0.3", BASE_TIME);
        List<ViewStats> stats = statsService.getStats(
                BASE_TIME.minusHours(1),
                BASE_TIME.plusHours(1),
                List.of("/event/1", "/event/2"),
                false
        );
        assertThat(stats, hasSize(2));
        assertThat(stats.stream().map(ViewStats::getUri).toList(),
                containsInAnyOrder("/event/1", "/event/2"));
    }

    @Test
    void shouldReturnEmptyListIfNoHitsInRange() {
        saveHit("app1", "/event/1", "192.168.0.1", BASE_TIME);
        List<ViewStats> stats = statsService.getStats(
                BASE_TIME.minusMinutes(10),
                BASE_TIME.minusMinutes(5),
                null,
                false
        );
        assertThat(stats, empty());
    }

    private void saveHit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHit hit = EndpointHit.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();
        statsService.saveHit(hit);
    }
}
