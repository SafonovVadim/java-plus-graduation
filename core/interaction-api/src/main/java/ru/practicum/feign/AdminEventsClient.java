package ru.practicum.feign;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.UpdateEventAdminRequest;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "event-service", contextId = "adminEvent", path = "/admin/events", configuration = FeignConfig.class)
public interface AdminEventsClient {

    @GetMapping()
    List<EventFullDto> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    );

    @PatchMapping("/{eventId}")
    EventFullDto updateEventByAdmin(@PathVariable Long eventId, @Valid @RequestBody UpdateEventAdminRequest updateRequest);
}
