package ru.practicum.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.dto.ViewStats;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.events.*;
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.dto.users.UserDto;
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
import ru.practicum.feign.PublicRequestClient;
import ru.practicum.feign.PublicUserClient;
import ru.practicum.feign.RequestsManagementClient;
import ru.practicum.mapper.EventsMapper;
import ru.practicum.mapper.RequestsMapper;
import ru.practicum.mapper.UserMapper;
import ru.practicum.repository.EventsRepository;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.events.dto.EventState.PENDING;
import static ru.practicum.mapper.CategoryMapper.toCategory;
import static ru.practicum.mapper.EventsMapper.toEventFullDto;
import static ru.practicum.mapper.RequestsMapper.toDto;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventsServiceImpl implements EventsService {
    private static final int MIN_HOURS_BEFORE_EVENT = 2;
    private final EventsRepository eventRepository;
    private final PublicCategoriesClient publicCategoriesClient;
    private final PublicUserClient publicUserClient;
    private final PublicRequestClient publicRequestClient;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    @SneakyThrows
    public EventFullDto saveEvent(NewEventDto newEventDto, Long userId) {
        log.info("Начинаем сохранение события для пользователя с ID: {}", userId);
        log.debug("Статус пре-модерации {}", newEventDto.getRequestModeration());
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        UserDto initiator = publicUserClient.getUser(userId);
        if (initiator == null) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        CategoryDto category = publicCategoriesClient.getCategoryById(newEventDto.getCategory());
        if (category == null) {
            throw new NotFoundException("Категория с id=" + newEventDto.getCategory() + " не найдена");
        }

        Event event = new Event();
        event.setAnnotation(newEventDto.getAnnotation());
        event.setCategory(category.getId());
        event.setDescription(newEventDto.getDescription());
        event.setEventDate(newEventDto.getEventDate());
        event.setLocation(newEventDto.getLocation());
        event.setPaid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false);
        event.setParticipantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0);
        event.setRequestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true);
        event.setTitle(newEventDto.getTitle());
        event.setInitiatorId(userId);
        event.setState(PENDING);
        event.setCreatedOn(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);
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
        User user = UserMapper.toUser(publicUserClient.getUser(userId));
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

//    private Map<Long, Long> getConfirmedRequestsMap(List<Event> events) {
//        if (events.isEmpty()) {
//            return Map.of();
//        }
//
//        List<Long> eventIds = events.stream()
//                .map(Event::getId)
//                .collect(Collectors.toList());
//
//        List<Object[]> results = publicRequestClient.countConfirmedRequestsByEventIds(eventIds, EventState.CONFIRMED);
//
//        return results.stream()
//                .collect(Collectors.toMap(
//                        row -> ((Number) row[0]).longValue(),
//                        row -> ((Number) row[1]).longValue()
//                ));
//    }

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

//    private Map<Long, Long> getRequestCounts(List<Long> eventIds) {
//        return publicRequestClient.countConfirmedRequestsByEventIds(eventIds, EventState.CONFIRMED).stream()
//                .collect(Collectors.toMap(
//                        r -> ((Number) r[0]).longValue(),
//                        r -> ((Number) r[1]).longValue()
//                ));
//    }

    @Override
    @Transactional
    @SneakyThrows
    public EventFullDto updateInactiveEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        log.info("Начало обновления события с ID: {} для пользователя с ID: {}", eventId, userId);
        log.debug("Dto {}", updateEventUserRequest);
        Event event = eventRepository.findEventByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id=" + eventId + " не найдено у пользователя с id=" + userId));

        if (event.getState() != PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Редактировать можно только отменённые или ожидающие модерацию события");
        }

        if (updateEventUserRequest.getAnnotation() != null) event.setAnnotation(updateEventUserRequest.getAnnotation());
        if (updateEventUserRequest.getCategory() != null) {
            CategoryDto category = publicCategoriesClient.getCategoryById(updateEventUserRequest.getCategory());
            if (category == null) {
                throw new NotFoundException("Категория с id=" + updateEventUserRequest.getCategory() + " не найдена");
            }
            event.setCategory(category.getId());
        }
        if (updateEventUserRequest.getDescription() != null)
            event.setDescription(updateEventUserRequest.getDescription());
        if (updateEventUserRequest.getEventDate() != null) {
            if (updateEventUserRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
            event.setEventDate(updateEventUserRequest.getEventDate());
        }
        if (updateEventUserRequest.getLocation() != null) event.setLocation(updateEventUserRequest.getLocation());
        if (updateEventUserRequest.getPaid() != null) event.setPaid(updateEventUserRequest.getPaid());
        if (updateEventUserRequest.getParticipantLimit() != null)
            event.setParticipantLimit(updateEventUserRequest.getParticipantLimit());
        if (updateEventUserRequest.getRequestModeration() != null)
            event.setRequestModeration(updateEventUserRequest.getRequestModeration());
        if (updateEventUserRequest.getTitle() != null) event.setTitle(updateEventUserRequest.getTitle());

        if (updateEventUserRequest.getStateAction() != null) {
            if (updateEventUserRequest.getStateAction().equals(StateAction.SEND_TO_REVIEW)) {
                event.setState(PENDING);
            } else if (updateEventUserRequest.getStateAction().equals(StateAction.CANCEL_REVIEW)) {
                event.setState(EventState.CANCELED);
            }
        }

        Event saved = eventRepository.save(event);
        Long confirmed = publicRequestClient.countByEventIdAndStatus(saved.getId(), EventState.CONFIRMED);
        saved.setConfirmedRequests(confirmed);
        return EventsMapper.toEventFullDto(saved);
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

        List<EventFullDto> eventFullDtos = events.stream()
                .peek(event -> event.setConfirmedRequests(
                        publicRequestClient.countByEventIdAndStatus(event.getId(), EventState.CONFIRMED)))
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

        if (!event.getInitiatorId().equals(userId)) {
            throw new ForbiddenActionException(
                    "Пользователь с ID " + userId + " не является инициатором события " + eventId
            );
        }

        log.debug("Событие найдено в БД: ID {}, заголовок '{}'", event.getId(), event.getTitle());

        // Получаем количество подтверждённых заявок
        long confirmedRequests = publicRequestClient.countByEventIdAndStatus(event.getId(), EventState.CONFIRMED);
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
        Event event = eventRepository.getEventById(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Только инициатор события может изменять статусы заявок");
        }

        EventState newStatus = request.getStatus();
        List<ParticipationRequest> requests = requestRepository.findAllById(request.getRequestIds());

        if (requests.size() != request.getRequestIds().size()) {
            throw new NotFoundException("Один или несколько запросов не найдены");
        }

        for (ParticipationRequest r : requests) {
            if (r.getStatus() != EventState.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок в состоянии PENDING");
            }
            if (!r.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Заявка относится к другому событию");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (newStatus == EventState.CONFIRMED) {
            Integer limit = eventRepository.getParticipantLimit(eventId);
            long available = (limit == null || limit == 0) ? Integer.MAX_VALUE : (limit - requestRepository.countByEventIdAndStatus(eventId, EventState.CONFIRMED));

            if (available <= 0) {
                throw new ConflictException("Достигнут лимит участников");
            }

            long toConfirm = Math.min(available, requests.size());
            for (int i = 0; i < requests.size(); i++) {
                ParticipationRequest req = requests.get(i);
                if (i < toConfirm) {
                    req.setStatus(EventState.CONFIRMED);
                    confirmed.add(toDto(req));
                } else {
                    req.setStatus(EventState.REJECTED);
                    rejected.add(toDto(req));
                }
            }
        } else {
            for (ParticipationRequest req : requests) {
                req.setStatus(EventState.REJECTED);
                rejected.add(toDto(req));
            }
        }

        requestRepository.saveAll(requests);
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);
        return result;
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        // 1. Проверяем существование события и принадлежность пользователю
        Event event = eventRepository.getEventById(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ForbiddenActionException("User is not the initiator of the event");
        }

        // 2. Получаем все заявки на событие
        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        // 3. Преобразуем в DTO
        return RequestsMapper.toDtoList(requests);
    }
}
