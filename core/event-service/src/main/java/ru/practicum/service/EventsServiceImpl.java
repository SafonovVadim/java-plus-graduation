package ru.practicum.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.dto.ViewStats;
import ru.practicum.dto.events.*;
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.entity.Category;
import ru.practicum.entity.Event;
import ru.practicum.entity.ParticipationRequest;
import ru.practicum.entity.User;
import ru.practicum.errors.exception.ConflictException;
import ru.practicum.errors.exception.EventCreationRuleException;
import ru.practicum.errors.exception.ForbiddenActionException;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.events.dto.EventState;
import ru.practicum.feign.PublicCategoriesClient;
import ru.practicum.feign.PublicUserClient;
import ru.practicum.mapper.EventsMapper;
import ru.practicum.mapper.RequestsMapper;
import ru.practicum.repository.EventsRepository;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.mapper.CategoryMapper.toCategory;
import static ru.practicum.mapper.EventsMapper.toEvent;
import static ru.practicum.mapper.EventsMapper.toEventFullDto;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventsServiceImpl implements EventsService {
    private static final int MIN_HOURS_BEFORE_EVENT = 2;
    private final EventsRepository eventRepository;
    private final PublicCategoriesClient publicCategoriesClient;
    private final PublicUserClient publicUserClient;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    public EventFullDto saveEvent(NewEventDto newEventDto, Long userId) {
        log.info("Начинаем сохранение события для пользователя с ID: {}", userId);
        log.debug("Статус пре-модерации {}", newEventDto.getRequestModeration());
        validateEventDate(newEventDto.getEventDate());
        User user = findUserById(userId);
        Category category = findCategoryById(newEventDto.getCategory());

        Event event = toEvent(newEventDto, user, category);
        Event savedEvent = eventRepository.save(event);

        savedEvent.setInitiator(user);
        savedEvent.setCategory(category);

        log.info("Событие успешно сохранено с ID: {} для пользователя с ID: {}", savedEvent.getId(), userId);
        return toEventFullDto(savedEvent);
    }

    /**
     * Валидирует дату события: проверяет, что она не раньше чем через MIN_HOURS_BEFORE_EVENT часов от текущего момента.
     *
     * @param eventDate дата события, которую нужно проверить
     * @throws EventCreationRuleException если дата события раньше минимально допустимой
     */
    private void validateEventDate(LocalDateTime eventDate) {
        LocalDateTime minEventDate = LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT);
        if (eventDate.isBefore(minEventDate)) {
            String message = "Событие не удовлетворяет правилам создания";

            log.warn("Попытка создать событие с датой раньше чем через {} часа. Дата события: {}, минимальная допустимая дата: {}",
                    MIN_HOURS_BEFORE_EVENT, eventDate, minEventDate);

            throw new EventCreationRuleException(
                    "eventDate",
                    eventDate,
                    message
            );
        }
        log.debug("Дата события прошла валидацию: {}", eventDate);
    }

    /**
     * Загружает категорию по ID из DTO и проверяет её существование.
     * Если категория не найдена, выбрасывает исключение с указанием ID.
     *
     * @param categoryId ID категории из NewEventDto
     * @return найденная сущность Category
     * @throws EventCreationRuleException если категория с указанным ID не найдена в БД
     */
    private Category findCategoryById(Long categoryId) {
        log.debug("Начинаем загрузку категории с ID: {}", categoryId);

        Category category = toCategory(publicCategoriesClient.getCategoryById(categoryId));


        log.debug("Категория успешно загружена: ID {}, название '{}'", category.getId(), category.getName());
        return category;
    }

    /**
     * Находит пользователя по ID или выбрасывает исключение, если пользователь не найден.
     *
     * @param userId ID пользователя, которого нужно найти
     * @return найденный пользователь
     * @throws NotFoundException если пользователь с указанным ID не найден
     */
    private User findUserById(Long userId) {
        User user = publicUserClient.getUser(userId);
        log.debug("Пользователь с ID {} найден: {}", userId, user.getName());
        return user;
    }

    private void setViewsToEvent(Event event) {
        List<ViewStats> stats = getStats(List.of("/events/" + event.getId()));
        long views = stats.stream().findFirst().map(ViewStats::getHits).orElse(0L);
        event.setViews(views);
    }

    private List<ViewStats> getStats(List<String> uris) {
        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.now();

        try {
            return statsClient.getStats(start, end, uris, true);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        List<Object[]> results = requestRepository.countConfirmedRequestsByEventIds(eventIds, EventState.CONFIRMED);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));
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

    private Map<Long, Long> getRequestCounts(List<Long> eventIds) {
        return requestRepository.countConfirmedRequestsByEventIds(eventIds, EventState.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));
    }

    @Override
    @Transactional
    public EventFullDto updateInactiveEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        log.info("Начало обновления события с ID: {} для пользователя с ID: {}", eventId, userId);
        log.debug("Dto {}", updateEventUserRequest);

        // 1. Находим событие по ID
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        // 2. Проверяем принадлежность события пользователю
        User user = event.getInitiator();
        if (!user.getId().equals(userId)) {
            throw new ForbiddenActionException("Пользователь с ID " + userId + " не является инициатором события " + eventId);
        }

        // 3. Проверяем статус события
        EventState currentState = event.getState();
        if (!currentState.equals(EventState.CANCELED) && !currentState.equals(EventState.PENDING)) {
            throw new ConflictException(
                    "Только отменённые события или события в состоянии ожидания модерации могут быть изменены. Текущий статус: " + currentState
            );
        }

        // 4. Обрабатываем stateAction, если указан
        StateAction stateAction = updateEventUserRequest.getStateAction();
        if (stateAction != null) {
            switch (stateAction) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ConflictException(
                            "Недопустимое значение stateAction: " + stateAction +
                                    ". Допустимые значения: SEND_TO_REVIEW, CANCEL_REVIEW"
                    );
            }
        }

        // 5. Применяем обновления полей (только не‑null)
        applyNonNullUpdates(event, updateEventUserRequest);

        // 6. Валидируем дату события
        LocalDateTime updateDate = updateEventUserRequest.getEventDate();
        if (updateDate != null) {
            validateEventDate(updateDate);
        } else if (stateAction == StateAction.SEND_TO_REVIEW) {
            validateEventDate(event.getEventDate());
        }

        // 7. Сохраняем и возвращаем результат
        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с ID: {} успешно обновлено", eventId);

        return toEventFullDto(updatedEvent);
    }

    /**
     * Применяет к сущности Event только те изменения из запроса, которые не равны null.
     */
    private <T extends UpdateEventRequest> void applyNonNullUpdates(Event event, T request) {
        // Общие поля для обоих типов запросов
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getLocation() != null) {
            event.setLocationLat(request.getLocation().getLat());
            event.setLocationLon(request.getLocation().getLon());
        }
        if (request.getCategory() != null) {
            Category category = toCategory(publicCategoriesClient.getCategoryById(request.getCategory()));
            event.setCategory(category);
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
    }

    @Override
    public List<EventFullDto> getUserEvents(Long userId, int from, int size) {
        log.debug("Начинаем поиск событий для пользователя с ID: {}, from: {}, size: {}", userId, from, size);


        User user = findUserById(userId);
        List<Event> events = eventRepository.findAllByInitiatorIdWithOffset(user.getId(), from, size);

        if (events.isEmpty()) {
            log.debug("Для пользователя с ID {} не найдено событий", userId);
            return Collections.emptyList();
        }

        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(events);

        List<EventFullDto> eventFullDtos = events.stream()
                .peek(event -> event.setConfirmedRequests(
                        confirmedRequests.getOrDefault(event.getId(), 0L)))
                .map(EventsMapper::toEventFullDto)
                .collect(Collectors.toList());

        log.info("Найдено {} событий для пользователя с ID {}", events.size(), userId);
        return eventFullDtos;
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        log.debug("Начинаем поиск события с ID: {} для пользователя с ID: {}", eventId, userId);

        // Находим пользователя — если не найден, будет выброшено исключение NotFoundException
        findUserById(userId);

        // Ищем событие по ID и проверяем принадлежность пользователю
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenActionException(
                    "Пользователь с ID " + userId + " не является инициатором события " + eventId
            );
        }

        log.debug("Событие найдено в БД: ID {}, заголовок '{}'", event.getId(), event.getTitle());

        // Получаем количество подтверждённых заявок
        long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), EventState.CONFIRMED);
        event.setConfirmedRequests(confirmedRequests);

        // Обновляем просмотры
        setViewsToEvent(event);

        log.info("Полные данные события подготовлены для возврата");
        return toEventFullDto(event);
    }

    @Override
    @SneakyThrows
    public EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request) {

        // 1. Проверяем существование события и принадлежность пользователю
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id=" + eventId + " не найдено у пользователя с id=" + userId));
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Редактировать можно только отменённые или ожидающие модерацию события");
        }
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenActionException("User is not the initiator of the event");
        }

        // 2. Проверяем условия пре‑модерации и лимита (400 BAD_REQUEST)
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new BadRequestException("Request moderation is not required for this event");
        }

        // 3. Находим заявки для обновления
        List<ParticipationRequest> requests = requestRepository.findAllById(request.getRequestIds());
        if (requests.isEmpty()) {
            throw new NotFoundException("No requests found for the given IDs");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, EventState.CONFIRMED);
        int newConfirmedCount = (int) confirmedCount + request.getRequestIds().size();

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if (newConfirmedCount > event.getParticipantLimit()) {
            List<ParticipationRequest> allPendingRequests = requestRepository
                    .findByEventIdAndStatus(eventId, EventState.PENDING);

            for (ru.practicum.entity.ParticipationRequest req : allPendingRequests) {
                req.setStatus(EventState.REJECTED);
                rejected.add(req);
            }
            requestRepository.saveAll(allPendingRequests);

            throw new ConflictException("The participant limit has been reached. All pending requests have been rejected.");
        } else {
            for (ParticipationRequest req : requests) {
                if (request.getStatus() == EventState.CONFIRMED) {
                    req.setStatus(EventState.CONFIRMED);
                    confirmed.add(req);
                } else if (request.getStatus() == EventState.REJECTED) {
                    req.setStatus(EventState.REJECTED);
                    rejected.add(req);
                }
            }
            requestRepository.saveAll(requests);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(RequestsMapper.toDtoList(confirmed))
                .rejectedRequests(RequestsMapper.toDtoList(rejected))
                .build();
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        // 1. Проверяем существование события и принадлежность пользователю
        Event event = eventRepository.getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenActionException("User is not the initiator of the event");
        }

        // 2. Получаем все заявки на событие
        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        // 3. Преобразуем в DTO
        return RequestsMapper.toDtoList(requests);
    }
}
