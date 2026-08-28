package ru.practicum.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import ru.practicum.EventsRepository;
import ru.practicum.StatsClient;
import ru.practicum.dto.ViewStats;
import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.NewEventDto;
import ru.practicum.dto.events.StateAction;
import ru.practicum.dto.events.UpdateEventUserRequest;
import ru.practicum.dto.users.UserDto;
import ru.practicum.entity.Event;
import ru.practicum.entity.User;
import ru.practicum.errors.exception.ConflictException;
import ru.practicum.errors.exception.ForbiddenActionException;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.events.dto.EventState;
import ru.practicum.feign.PublicCategoriesClient;
import ru.practicum.feign.PublicRequestClient;
import ru.practicum.feign.PublicUserClient;
import ru.practicum.feign.RequestsManagementClient;
import ru.practicum.mapper.EventsMapper;
import ru.practicum.mapper.UserMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.events.dto.EventState.PENDING;
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
    private final PublicRequestClient publicRequestClient;
    private final RequestsManagementClient requestsManagementClient;
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
                throw new ConflictException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
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


}
