package ru.practicum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
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
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    public StatsClientImpl(
            RestTemplate restTemplate,
            DiscoveryClient discoveryClient,
            @Value("${stats.service.id:stats-server}") String statsServiceId
    ) {
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.retryTemplate = createRetryTemplate();
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);
        template.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
        if (instances == null || instances.isEmpty()) {
            throw new RuntimeException("Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId);
        }
        return instances.getFirst();
    }

    @Override
    public void hit(EndpointHit hit) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        String url = String.format("http://%s:%s/hit", instance.getHost(), instance.getPort());
        restTemplate.postForEntity(url, hit, Void.class);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, Boolean unique) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
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
