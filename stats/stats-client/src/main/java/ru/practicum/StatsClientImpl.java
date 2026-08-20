package ru.practicum;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
public class StatsClientImpl implements StatsClient {

    private final RestTemplate restTemplate;
    private final ServiceInstanceProvider serviceInstanceProvider;
    private final RetryTemplate retryTemplate;

    public StatsClientImpl(
            RestTemplate restTemplate,
            ServiceInstanceProvider serviceInstanceProvider,
            RetryTemplate retryTemplate
    ) {
        this.restTemplate = restTemplate;
        this.serviceInstanceProvider = serviceInstanceProvider;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public void hit(EndpointHit hit) {
        ServiceInstance instance = retryTemplate.execute(ctx -> serviceInstanceProvider.getInstance());
        String url = String.format("http://%s:%s/hit", instance.getHost(), instance.getPort());
        restTemplate.postForEntity(url, hit, Void.class);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, Boolean unique) {
        ServiceInstance instance = retryTemplate.execute(ctx -> serviceInstanceProvider.getInstance());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
                        String.format("http://%s:%s/stats", instance.getHost(), instance.getPort()))
                .queryParam("start", formatter.format(start))
                .queryParam("end", formatter.format(end))
                .queryParam("unique", unique != null && unique);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", String.join(",", uris));
        }

        URI uri = builder.build().encode().toUri();
        ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(uri, ViewStats[].class);
        return List.of(Objects.requireNonNull(response.getBody()));
    }
}
