package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "request-service", contextId = "publicRequest", path = "/public/requests", configuration = FeignConfig.class)
public interface PublicRequestClient {
    @GetMapping("/event/{eventId}/count")
    Long countByEventIdAndStatus(@PathVariable Long eventId, @RequestParam ru.practicum.events.dto.EventState status);

    @GetMapping("/exists")
    Boolean existsByEventIdAndRequesterIdAndStatus(
            @RequestParam("eventId") Long eventId,
            @RequestParam("userId") Long userId,
            @RequestParam("status") ru.practicum.events.dto.EventState status);
}
