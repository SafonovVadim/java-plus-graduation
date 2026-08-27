package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.NewEventDto;
import ru.practicum.dto.events.UpdateEventUserRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.feign.PrivateEventsClient;
import ru.practicum.service.EventsService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivatePrivateEventsController implements PrivateEventsClient {

    private final EventsService eventsService;

    @Override
    public EventFullDto addEvent(
            @Valid @RequestBody NewEventDto newEventDto,
            @PathVariable @Positive Long userId) {

        return eventsService.saveEvent(newEventDto, userId);
    }

    @Override
    public EventFullDto updateEvent(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {

        return eventsService.updateInactiveEvent(userId, eventId, updateEventUserRequest);
    }

    @Override
    public List<EventFullDto> getUserEvents(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        return eventsService.getUserEvents(userId, from, size);
    }

    @Override
    public EventFullDto getUserEventById(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId) {
        return eventsService.getUserEventById(userId, eventId);
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(@PathVariable Long userId, @PathVariable Long eventId, @RequestBody EventRequestStatusUpdateRequest request) {
        return eventsService.updateRequestStatuses(userId, eventId, request);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventsService.getEventRequests(userId, eventId);
    }
}
