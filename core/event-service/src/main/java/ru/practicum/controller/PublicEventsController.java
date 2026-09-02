package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.CollectorClient;
import ru.practicum.StatsClient;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.entity.Event;
import ru.practicum.feign.PublicEventsClient;
import ru.practicum.service.PublicEventsService;
import ru.practicum.service.collector.ActionTypeProto;
import ru.practicum.service.collector.UserActionProto;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventsController implements PublicEventsClient {

    private final PublicEventsService eventService;
    private final StatsClient statsClient;
    private final CollectorClient collectorClient;

    @Override
    public ResponseEntity<List<EventShortDto>> getEvents(
            @RequestParam(required = false)
            @Size(max = 1000, message = "Text length must be less than or equal to 1000 characters")
            String text,

            @RequestParam(required = false)
            List<@Positive(message = "Category IDs must be positive numbers") Long> categories,

            @RequestParam(required = false) Boolean paid,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeStart,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeEnd,

            @RequestParam(defaultValue = "false") Boolean onlyAvailable,

            @RequestParam(defaultValue = "EVENT_DATE")
            @Pattern(regexp = "EVENT_DATE|RATING", message = "Sort must be either 'EVENT_DATE' or 'RATING'")
            String sort,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "From must be greater than or equal to 0")
            Integer from,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size must be greater than 0")
            @Max(value = 1000, message = "Size must be less than or equal to 1000")
            Integer size
    ) {

        List<EventShortDto> events = eventService.getPublishedEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable,
                sort.equals("RATING"), from, size
        );

        return ResponseEntity.ok(events);
    }

    @Override
    public ResponseEntity<EventFullDto> getEventByIdFull(
            @PathVariable Long id,
            @RequestHeader("X-EWM-USER-ID") Long userId,
            HttpServletRequest request
    ) {
        UserActionProto userActionProto = UserActionProto.newBuilder()
                .setEventId(id.intValue())
                .setUserId(userId.intValue())
                .setActionType(ActionTypeProto.ACTION_VIEW)
                .build();
        collectorClient.sendUserAction(userActionProto);

        EventFullDto event = eventService.getPublishedEventById(id);
        return ResponseEntity.ok(event);
    }

    @Override
    public EventShortDto getEventShort(@PathVariable Long eventId) {
        return eventService.getEventShort(eventId);
    }

    @Override
    public Event getEvent(@PathVariable Long eventId) {
        return eventService.getEvent(eventId);
    }

    @Override
    public Boolean checkEventByCategory(@PathVariable Long categoryId) {
        return eventService.getEventByCategory(categoryId);
    }

    @Override
    public ResponseEntity<Void> addLike(Long eventId, Long userId) {
        return eventService.addLike(eventId, userId);
    }

    @Override
    public ResponseEntity<List<EventShortDto>> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId) {
        List<EventShortDto> recommendations = eventService.getRecommendations(userId, 10);
        return ResponseEntity.ok(recommendations);
    }
}
