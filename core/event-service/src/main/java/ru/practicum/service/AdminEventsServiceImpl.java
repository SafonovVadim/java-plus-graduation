package ru.practicum.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.AnalyzerClient;
import ru.practicum.EventsRepository;
import ru.practicum.StatsClient;
import ru.practicum.dto.ViewStats;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.UpdateEventAdminRequest;
import ru.practicum.entity.Event;
import ru.practicum.errors.exception.ConflictException;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.errors.exception.BadRequestException;
import ru.practicum.feign.PublicCategoriesClient;
import ru.practicum.feign.PublicRequestClient;
import ru.practicum.mapper.EventsMapper;
import ru.practicum.service.dashboard.RecommendedEventProto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.dto.events.StateAction.PUBLISH_EVENT;
import static ru.practicum.dto.events.StateAction.REJECT_EVENT;
import static ru.practicum.events.dto.EventState.CONFIRMED;
import static ru.practicum.mapper.EventsMapper.toEventFullDto;

@Service
@RequiredArgsConstructor
public class AdminEventsServiceImpl implements AdminEventsService {
    private final EntityManager entityManager;
    private final AnalyzerClient analyzerClient;
    private final EventsRepository eventsRepository;
    private final PublicCategoriesClient publicCategoriesClient;
    private final PublicRequestClient publicRequestClient;

    @Override
    public List<EventFullDto> getEvents(
            List<Long> userIds,
            List<String> states,
            List<Long> categoryIds,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> query = cb.createQuery(Event.class);
        Root<Event> root = query.from(Event.class);

        List<Predicate> predicates = new ArrayList<>();

        if (userIds != null && !userIds.isEmpty()) {
            predicates.add(root.get("initiatorId").in(userIds));
        }

        if (states != null && !states.isEmpty()) {
            List<ru.practicum.events.dto.EventState> stateEnums = states.stream()
                    .map(ru.practicum.events.dto.EventState::valueOf)
                    .collect(Collectors.toList());
            predicates.add(root.get("state").in(stateEnums));
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            predicates.add(root.get("category").in(categoryIds));
        }

        if (rangeStart != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }
        if (rangeEnd != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }

        query.select(root).where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("eventDate")));

        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Event> events = entityManager.createQuery(query)
                .setFirstResult((int) pageRequest.getOffset())
                .setMaxResults(pageRequest.getPageSize())
                .getResultList();

        for (Event event : events) {
            Long confirmed = publicRequestClient.countByEventIdAndStatus(event.getId(), CONFIRMED);
            event.setConfirmedRequests(confirmed);
        }

        setRatingToEvents(events);

        return events.stream()
                .map(EventsMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getCategory() != null) {
            CategoryDto category = publicCategoriesClient.getCategoryById(request.getCategory());
            if (category == null) {
                throw new NotFoundException("Категория с id=" + request.getCategory() + " не найдена");
            }
            event.setCategory(category.getId());
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new BadRequestException("Дата начала события должна быть не ранее чем за час от даты публикации");
            }
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) event.setLocation(request.getLocation());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals(PUBLISH_EVENT)) {
                if (event.getState() != ru.practicum.events.dto.EventState.PENDING) {
                    throw new ConflictException("Опубликовать можно только событие в статусе PENDING");
                }
                event.setState(ru.practicum.events.dto.EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction().equals(REJECT_EVENT)) {
                if (event.getState() == ru.practicum.events.dto.EventState.PUBLISHED) {
                    throw new ConflictException("Нельзя отклонить уже опубликованное событие");
                }
                event.setState(ru.practicum.events.dto.EventState.CANCELED);
            }
        }

        Event saved = eventsRepository.save(event);
        Long confirmed = publicRequestClient.countByEventIdAndStatus(saved.getId(), CONFIRMED);
        saved.setConfirmedRequests(confirmed);
        setRatingToEvents(List.of(saved));
        return toEventFullDto(saved);
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
