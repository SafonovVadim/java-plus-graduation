package ru.practicum.service;


import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;

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

}
