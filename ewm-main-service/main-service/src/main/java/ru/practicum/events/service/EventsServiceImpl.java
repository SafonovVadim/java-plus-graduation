    package ru.practicum.events.service;

    import jakarta.persistence.EntityManager;
    import jakarta.persistence.criteria.CriteriaBuilder;
    import jakarta.persistence.criteria.CriteriaQuery;
    import jakarta.persistence.criteria.Predicate;
    import jakarta.persistence.criteria.Root;
    import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.domain.Specification;
    import org.springframework.stereotype.Service;
    import ru.practicum.StatsClient;
    import ru.practicum.categories.Category;
    import ru.practicum.categories.CategoryRepository;
    import ru.practicum.dto.ViewStats;
    import ru.practicum.error.exception.ForbiddenActionException;
    import ru.practicum.events.*;
    import ru.practicum.events.dto.*;
    import ru.practicum.error.exception.ConflictException;
    import ru.practicum.error.exception.EventCreationRuleException;
    import ru.practicum.error.exception.NotFoundException;
    import ru.practicum.requests.RequestRepository;
    import ru.practicum.user.User;
    import ru.practicum.user.UserRepository;

    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.List;
    import java.util.Map;
    import java.util.stream.Collectors;

    import static ru.practicum.events.EventsMapper.*;

    @Service
    @RequiredArgsConstructor
    @Transactional
    @Slf4j
    public class EventsServiceImpl implements EventsService {
        private static final int MIN_HOURS_BEFORE_EVENT = 2;
        private final EventsRepository eventRepository;
        private final CategoryRepository categoryRepository;
        private final RequestRepository requestRepository;
        private final UserRepository userRepository;
        private final StatsClient statsClient;
        private final EntityManager entityManager;

        @Override
        public EventFullDto saveEvent(NewEventDto newEventDto, Long userId) {
            log.info("Начинаем сохранение события для пользователя с ID: {}", userId);
            log.debug("Статус пре-модерации {}",newEventDto.getRequestModeration());
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

            Map<Long, Long> requestCounts = getRequestCounts(events.stream().map(Event::getId).toList());

            return events.stream()
                    .map(event -> toShortEventDto(event, requestCounts.getOrDefault(event.getId(), 0L)))
                    .collect(Collectors.toList());
        }

        @Override
        public EventFullDto getPublishedEventById(Long id) {
            Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                    .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

            long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), EventState.CONFIRMED);
            event.setConfirmedRequests(confirmedRequests);

            setViewsToEvents(List.of(event));

            return toEventFullDto(event);
        }

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
                predicates.add(root.get("initiator").get("id").in(userIds));
            }

            if (states != null && !states.isEmpty()) {
                List<EventState> stateEnums = states.stream()
                        .map(EventState::valueOf)
                        .collect(Collectors.toList());
                predicates.add(root.get("state").in(stateEnums));
            }

            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
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

            Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(events);

            setViewsToEvents(events);

            return events.stream()
                    .peek(event -> event.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L)))
                    .map(EventsMapper::toEventFullDto)
                    .collect(Collectors.toList());
        }

        @Override
        public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

            if (request.getStateAction() != null) {
                switch (request.getStateAction()) {
                    case PUBLISH_EVENT -> {
                        if (!event.getState().equals(EventState.PENDING)) {
                            throw new ConflictException("Cannot publish event in state: " + event.getState());
                        }
                        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                            throw new ConflictException("Event must be at least 1 hour after current time to be published");
                        }
                        event.setState(EventState.PUBLISHED);
                        event.setPublishedOn(LocalDateTime.now());
                    }
                    case REJECT_EVENT -> {
                        if (event.getState().equals(EventState.PUBLISHED)) {
                            throw new ConflictException("Cannot reject published event");
                        }
                        event.setState(EventState.CANCELED);
                    }
                }
            }

            applyNonNullUpdates(event, request);

            Event saved = eventRepository.save(event);
            saved.setConfirmedRequests(requestRepository.countByEventIdAndStatus(saved.getId(), EventState.CONFIRMED));

            setViewsToEvent(saved);

            return EventsMapper.toEventFullDto(saved);
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

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> {
                        String message = "Категория с ID " + categoryId + " не найдена в базе данных";
                        log.warn(message);
                        return new EventCreationRuleException(
                                "categoryId",
                                categoryId,
                                message
                        );
                    });

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
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException(
                            "Пользователь с ID " + userId + " не найден"));
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
                Category category = categoryRepository.findById(request.getCategory())
                        .orElseThrow(() -> new NotFoundException("Category with id=" + request.getCategory() + " was not found"));
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

            // УБРАЛИ вызов setViewsToEvents(events) — статистика не нужна для этого метода


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
            User user = findUserById(userId);

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
    }
