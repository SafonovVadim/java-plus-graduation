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
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "events-management", path = "/users/{userId}/events", configuration = FeignConfig.class)
public interface PrivateEventsClient {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EventFullDto addEvent(
            @Valid @RequestBody NewEventDto newEventDto,
            @PathVariable @Positive Long userId);

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto updateEvent(
            Long userId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest);

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<EventFullDto> getUserEvents(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size);

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto getUserEventById(
            Long userId,
            @PathVariable @Positive Long eventId);

    @PatchMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    EventRequestStatusUpdateResult updateRequestStatus(
            Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request);

    @GetMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    List<ParticipationRequestDto> getEventRequests(Long userId, @PathVariable Long eventId);

}
