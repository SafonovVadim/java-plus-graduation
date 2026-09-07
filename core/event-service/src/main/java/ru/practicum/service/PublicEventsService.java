package ru.practicum.service;


import org.springframework.http.ResponseEntity;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.entity.Event;

import java.time.LocalDateTime;
import java.util.List;

public interface PublicEventsService {

    List<EventShortDto> getPublishedEvents(
            String text,
            List<Long> categoryIds,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            boolean sortByViews,
            int from,
            int size
    );

    EventFullDto getPublishedEventById(Long id);

    EventShortDto getEventShort(Long id);

    Event getEvent(Long id);

    Boolean getEventByCategory(Long categoryId);

    List<EventShortDto> getRecommendations(Long userId, int limit);

    ResponseEntity<Void> addLike(Long eventId, Long userId);

}
