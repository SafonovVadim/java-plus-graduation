package ru.practicum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceInstanceProvider {

    private final DiscoveryClient discoveryClient;
    private final String statsServiceId;

    public ServiceInstanceProvider(
            DiscoveryClient discoveryClient,
            @Value("${stats.service.id:stats-server}") String statsServiceId
    ) {
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
    }

    public ServiceInstance getInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
        if (instances == null || instances.isEmpty()) {
            throw new RuntimeException("Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId);
        }
        return instances.getFirst();
    }
}
