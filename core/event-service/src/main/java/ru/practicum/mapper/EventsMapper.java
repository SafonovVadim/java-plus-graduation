package ru.practicum.mapper;

import ru.practicum.dto.categories.CategoryDto;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.dto.events.NewEventDto;
import ru.practicum.dto.users.UserDto;
import ru.practicum.dto.users.UserShortDto;
import ru.practicum.entity.Event;
import ru.practicum.events.dto.EventState;

import java.time.LocalDateTime;

import static ru.practicum.events.dto.EventState.PENDING;


public class EventsMapper {

    public static EventShortDto toShortEventDto(Event event, Long confirmedRequests) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(new CategoryDto() {{
            setId(event.getCategory());
        }});
        dto.setConfirmedRequests(confirmedRequests);
        dto.setEventDate(event.getEventDate().format(Constants.FORMATTER));
        dto.setInitiator(new UserShortDto() {{
            setId(event.getInitiatorId());
        }});
        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        dto.setViews(event.getViews());
        return dto;
    }

    public static EventShortDto toShortEventDto(Event event) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(new CategoryDto() {{
            setId(event.getCategory());
        }});
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setEventDate(event.getEventDate().format(Constants.FORMATTER));
        dto.setInitiator(new UserShortDto() {{
            setId(event.getInitiatorId());
        }});
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
        dto.setCategory(new CategoryDto() {{
            setId(event.getCategory());
        }});
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setCreatedOn(format(event.getCreatedOn()));
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate().format(Constants.FORMATTER));
        dto.setInitiator(new UserShortDto() {{
            setId(event.getInitiatorId());
        }});
        dto.setLocation(event.getLocation());
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
     * Преобразует EventFullDto в сущность Event.
     *
     * @param dto DTO с полными данными события
     * @return сущность Event, готовая для сохранения в БД
     */
    public static Event toEvent(EventFullDto dto) {
        return Event.builder()
                .id(dto.getId())
                .annotation(dto.getAnnotation())
                .category(dto.getCategory().getId())
                .confirmedRequests(dto.getConfirmedRequests())
                .description(dto.getDescription())
                .eventDate(LocalDateTime.parse(dto.getEventDate(), Constants.FORMATTER))
                .initiatorId(dto.getInitiator().getId())
                .location(dto.getLocation())
                .paid(dto.getPaid())
                .participantLimit(dto.getParticipantLimit())
                .publishedOn(dto.getPublishedOn() != null ? LocalDateTime.parse(dto.getPublishedOn(), Constants.FORMATTER) : null)
                .requestModeration(dto.getRequestModeration())
                .state(EventState.valueOf(dto.getState()))
                .title(dto.getTitle())
                .views(dto.getViews())
                .build();
    }

    public static Event createEventDto(NewEventDto newEventDto, Long categoryId, Long userId) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(categoryId)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .location(newEventDto.getLocation())
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true)
                .title(newEventDto.getTitle())
                .initiatorId(userId)
                .confirmedRequests(0L)
                .state(PENDING)
                .createdOn(LocalDateTime.now())
                .build();
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(Constants.FORMATTER) : null;
    }
}
