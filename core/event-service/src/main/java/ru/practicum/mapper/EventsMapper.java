package ru.practicum.mapper;

import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.dto.events.Location;
import ru.practicum.dto.events.NewEventDto;
import ru.practicum.entity.Category;
import ru.practicum.entity.Event;
import ru.practicum.entity.User;
import ru.practicum.events.dto.EventState;

import java.time.LocalDateTime;

import static ru.practicum.mapper.CategoryMapper.toCategoryDto;
import static ru.practicum.mapper.UserMapper.toShortDto;


public class EventsMapper {

    public static EventShortDto toShortEventDto(Event event, Long confirmedRequests) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(toCategoryDto(event.getCategory()));
        dto.setConfirmedRequests(confirmedRequests);
        dto.setEventDate(event.getEventDate().format(Constance.FORMATTER));
        dto.setInitiator(toShortDto(event.getInitiator()));
        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        dto.setViews(event.getViews());
        return dto;
    }

    public static EventShortDto toShortEventDto(Event event) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(toCategoryDto(event.getCategory()));
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setEventDate(event.getEventDate().format(Constance.FORMATTER));
        dto.setInitiator(toShortDto(event.getInitiator()));
        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        dto.setViews(event.getViews());
        return dto;
    }

    public static EventShortDto toShortEventDtoById(Long eventId, Long confirmedRequests) {
        EventShortDto dto = new EventShortDto();
        dto.setId(eventId);
        dto.setConfirmedRequests(confirmedRequests);
        return dto;
    }

    public static EventFullDto toEventFullDto(Event event) {
        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(toCategoryDto(event.getCategory()));
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setCreatedOn(format(event.getCreatedOn()));
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate().format(Constance.FORMATTER));
        dto.setInitiator(new UserMapper().toShortDto(event.getInitiator()));
        dto.setLocation(new Location(event.getLocationLat(), event.getLocationLon()));
        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setPublishedOn(event.getPublishedOn() != null ? format(event.getPublishedOn()) : null);
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState().name());
        dto.setTitle(event.getTitle());
        dto.setViews(event.getViews());
        return dto;
    }

    /**
     * Преобразует DTO нового события в сущность Event.
     *
     * @param dto  DTO с данными нового события
     * @param user пользователь-инициатор события
     * @return сущность Event, готовая для сохранения в БД
     */
    public static Event toEvent(NewEventDto dto, User user, Category category) {
        return Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .description(dto.getDescription())
                .title(dto.getTitle())
                .eventDate(dto.getEventDate())
                .paid(dto.getPaid())
                .participantLimit(dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration())
                .locationLat(dto.getLocation().getLat())
                .locationLon(dto.getLocation().getLon())
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .initiator(user)
                .confirmedRequests(0L)
                .views(0L)
                .build();
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(Constance.FORMATTER) : null;
    }
}
