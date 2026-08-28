package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.NewEventDto;
import ru.practicum.dto.events.UpdateEventUserRequest;
import ru.practicum.feign.PrivateEventsClient;
import ru.practicum.service.EventsService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventsController implements PrivateEventsClient {

    private final EventsService eventsService;

    private Long extractUserId() {
        String path = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getPathInfo();
        return Long.parseLong(path.substring("/users/".length()));
    }

    @Override
    public EventFullDto addEvent(
            @Valid @RequestBody NewEventDto newEventDto) {

        return eventsService.saveEvent(newEventDto, extractUserId());
    }

    @Override
    public EventFullDto updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {

        return eventsService.updateInactiveEvent(extractUserId(), eventId, updateEventUserRequest);
    }

    @Override
    public List<EventFullDto> getUserEvents(
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        return eventsService.getUserEvents(extractUserId(), from, size);
    }

    @Override
    public EventFullDto getUserEventById(
            @PathVariable Long eventId) {
        return eventsService.getUserEventById(extractUserId(), eventId);
    }
}

