package ru.practicum.events.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.NewEventDto;
import ru.practicum.events.dto.UpdateEventUserRequest;
import ru.practicum.events.service.EventsService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
public class PrivateEventController {

    private final EventsService eventsService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(
            @Valid @RequestBody NewEventDto newEventDto,
                @PathVariable @Positive Long userId) {

        log.info("Получен запрос на создание нового события для пользователя с ID: {}. Заголовок события: '{}'", userId, newEventDto.getTitle());
        log.debug("Полные данные события, полученные от клиента: {}", newEventDto);

        EventFullDto savedEvent = eventsService.saveEvent(newEventDto, userId);

        log.info("Событие успешно создано с ID: {} для пользователя с ID: {}", savedEvent.getId(), userId);
        log.debug("Полные данные сохранённого события: {}", savedEvent);

        return savedEvent;
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {

        log.info("Получен запрос на обновление события с ID: {} для пользователя с ID: {}", eventId, userId);
        log.debug("Данные для обновления события: {}", updateEventUserRequest);

        EventFullDto updatedEvent = eventsService.updateInactiveEvent(userId, eventId, updateEventUserRequest);

        log.info("Событие с ID: {} успешно обновлено для пользователя с ID: {}", eventId, userId);
        log.debug("Полные данные обновлённого события: {}", updatedEvent);

        return updatedEvent;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> getUserEvents(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        log.info("Получен запрос на получение событий пользователя с ID: {}, from: {}, size: {}", userId, from, size);

        List<EventFullDto> userEvents = eventsService.getUserEvents(userId, from, size);

        log.info("Для пользователя с ID {} найдено {} событий", userId, userEvents.size());
        return userEvents;
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getUserEventById(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId) {


        log.info("Получен запрос на получение события с ID: {} для пользователя с ID: {}", eventId, userId);

        EventFullDto event = eventsService.getUserEventById(userId, eventId);

        log.info("Событие с ID: {} успешно найдено для пользователя с ID: {}", eventId, userId);
        log.debug("Полные данные найденного события: {}", event);

        return event;
    }
}
