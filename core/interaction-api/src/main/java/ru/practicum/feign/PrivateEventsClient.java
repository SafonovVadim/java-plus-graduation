package ru.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.NewEventDto;
import ru.practicum.dto.events.UpdateEventUserRequest;

import java.util.List;

@FeignClient(name = "event-service",contextId = "privateEvenClient", path = "/users/{userId}/events", configuration = FeignConfig.class)
public interface PrivateEventsClient {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EventFullDto addEvent(
            @Valid @RequestBody NewEventDto newEventDto);

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest);

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<EventFullDto> getUserEvents(
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size);

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto getUserEventById(
            @PathVariable Long eventId);

}
