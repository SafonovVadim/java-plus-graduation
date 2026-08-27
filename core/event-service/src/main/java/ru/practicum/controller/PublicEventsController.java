package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.StatsClient;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.feign.PublicEventsClient;
import ru.practicum.service.PublicEventsService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventsController implements PublicEventsClient {

    private final PublicEventsService eventService;
    private final StatsClient statsClient;

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
            @Pattern(regexp = "EVENT_DATE|VIEWS", message = "Sort must be either 'EVENT_DATE' or 'VIEWS'")
            String sort,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "From must be greater than or equal to 0")
            Integer from,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size must be greater than 0")
            @Max(value = 1000, message = "Size must be less than or equal to 1000")
            Integer size,

            HttpServletRequest request
    ) {
        EndpointHit hit = EndpointHit.builder()
                .app("events-management")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.hit(hit);

        List<EventShortDto> events = eventService.getPublishedEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable,
                sort.equals("VIEWS"), from, size
        );

        return ResponseEntity.ok(events);
    }

    @Override
    public ResponseEntity<EventFullDto> getEventByIdFull(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        EndpointHit hit = EndpointHit.builder()
                .app("events-management")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.hit(hit);

        EventFullDto event = eventService.getPublishedEventById(id);
        return ResponseEntity.ok(event);
    }

    @Override
    public EventShortDto getEventShort(@PathVariable Long eventId) {
        return eventService.getEventShort(eventId);
    }
}
