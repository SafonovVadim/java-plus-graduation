package ru.practicum.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.AnalyzerClient;
import ru.practicum.CollectorClient;
import ru.practicum.EventsRepository;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.entity.Event;
import ru.practicum.errors.exception.BadRequestException;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.events.dto.EventState;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
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
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PublicEventsServiceImpl implements PublicEventsService {
    private final EventsRepository eventRepository;
    private final EventsRepository eventsRepository;
    private final PublicRequestClient publicRequestClient;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;

    @Override
    public List<EventShortDto> getPublishedEvents(
            String text,
            List<Long> categoryIds,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            boolean sortByRating,
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

        if (sortByRating) {
            events.sort((e1, e2) -> Double.compare(e2.getRating(), e1.getRating()));
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
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + id + " не найдено");
        }

        long confirmedRequests = publicRequestClient.countByEventIdAndStatus(event.getId(), CONFIRMED);
        event.setConfirmedRequests(confirmedRequests);

        setRatingToEvents(List.of(event));

        return toEventFullDto(event);
    }

    @Override
    public EventShortDto getEventShort(Long id) {
        return toShortEventDto(eventsRepository.findById(id).orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено")));
    }

    @Override
    public Event getEvent(Long id) {
        return eventsRepository.findById(id).orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));
    }


    @Override
    public Boolean getEventByCategory(Long categoryId) {
        return eventsRepository.existsByCategory(categoryId);
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, int limit) {
        log.info("Получение рекомендаций для пользователя {}", userId);
        return analyzerClient.getRecommendationsForUser(userId.intValue(), limit)
                .map(proto -> {
                    Event event = eventsRepository.findById((long) proto.getEventId())
                            .orElseThrow(() -> new NotFoundException(
                                    "Событие с id=" + proto.getEventId() + " не найдено"));
                    return toShortEventDto(event);
                })
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<Void> addLike(Long eventId, Long userId) {
        if (!publicRequestClient.existsByEventIdAndRequesterIdAndStatus(eventId, userId, CONFIRMED)) {
            throw new BadRequestException("Пользователь не посещал данное мероприятие");
        }

        UserActionProto userActionProto = UserActionProto.newBuilder()
                .setEventId(eventId.intValue())
                .setUserId(userId.intValue())
                .setActionType(ActionTypeProto.ACTION_LIKE)
                .build();
        collectorClient.sendUserAction(userActionProto);

        return ResponseEntity.ok().build();
    }

    private void setRatingToEvents(List<Event> events) {
        if (events.isEmpty()) return;

        int[] eventIds = events.stream()
                .mapToInt(e -> e.getId().intValue())
                .toArray();

        Map<Integer, Double> ratingMap = analyzerClient.getInteractionsCount(eventIds)
                .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getPredictedRating));

        events.forEach(event -> {
            Double rating = ratingMap.getOrDefault(event.getId().intValue(), 0.0);
            event.setRating(rating);
        });
    }
}
