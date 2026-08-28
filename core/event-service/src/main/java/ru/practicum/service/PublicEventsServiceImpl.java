package ru.practicum.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.practicum.EventsRepository;
import ru.practicum.StatsClient;
import ru.practicum.dto.ViewStats;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.entity.Event;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.events.dto.EventState;
import ru.practicum.feign.PublicRequestClient;
import ru.practicum.mapper.EventsMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.events.dto.EventState.CONFIRMED;
import static ru.practicum.mapper.EventsMapper.toEventFullDto;
import static ru.practicum.mapper.EventsMapper.toShortEventDto;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PublicEventsServiceImpl implements PublicEventsService {
    private final EventsRepository eventRepository;
    private final StatsClient statsClient;
    private final EventsRepository eventsRepository;
    private final PublicRequestClient publicRequestClient;


    @Override
    public List<EventShortDto> getPublishedEvents(
            String text,
            List<Long> categoryIds,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            boolean sortByViews,
            int from,
            int size
    ) {
        Pageable pageable = PageRequest.of(from / size, size);

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        Specification<Event> spec = Specification.where(EventSpecification.hasStatePublished())
                .and(EventSpecification.hasTextInAnnotationOrDescription(text))
                .and(EventSpecification.belongsToCategories(categoryIds))
                .and(EventSpecification.isPaid(paid))
                .and(EventSpecification.isWithinRange(rangeStart, rangeEnd));

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events.removeIf(event -> event.getParticipantLimit() > 0 &&
                    event.getConfirmedRequests() >= event.getParticipantLimit());
        }

        if (sortByViews) {
            events.sort((e1, e2) -> Long.compare(e2.getViews(), e1.getViews()));
        }

        return events.stream()
                .map(event -> {
                    Long confirmed = publicRequestClient.countByEventIdAndStatus(event.getId(), CONFIRMED);
                    return EventsMapper.toShortEventDto(event, confirmed);
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublishedEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + id + " не найдено");
        }

        long confirmedRequests = publicRequestClient.countByEventIdAndStatus(event.getId(), CONFIRMED);
        event.setConfirmedRequests(confirmedRequests);

        setViewsToEvents(List.of(event));

        return toEventFullDto(event);
    }

    @Override
    public EventShortDto getEventShort(Long id) {
        return toShortEventDto(eventsRepository.findById(id).orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found")));
    }

    @Override
    public Event getEvent(Long id) {
        return eventsRepository.findById(id).orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));
    }

    private void setViewsToEvents(List<Event> events) {
        if (events.isEmpty()) return;

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        LocalDateTime start = LocalDateTime.now().minusYears(1);
        LocalDateTime end = LocalDateTime.now();

        List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

        Map<String, Long> viewsMap = stats.stream()
                .collect(Collectors.toMap(
                        ViewStats::getUri,
                        ViewStats::getHits
                ));

        events.forEach(event -> {
            String uri = "/events/" + event.getId();
            Long views = viewsMap.getOrDefault(uri, 0L);
            event.setViews(views);
        });
    }

    @Override
    public Boolean getEventByCategory(Long categoryId) {
        return eventsRepository.existsByCategory(categoryId);
    }
}
