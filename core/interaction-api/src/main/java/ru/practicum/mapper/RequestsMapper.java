package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.entity.Event;
import ru.practicum.entity.ParticipationRequest;
import ru.practicum.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.mapper.Constance.FORMATTER;


@Component
public class RequestsMapper {

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .created(request.getCreated().format(FORMATTER))
                .event(request.getEvent().getId())
                .id(request.getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus())
                .build();
    }

    public static ParticipationRequest toParticipationRequest(ParticipationRequestDto dto, Event event, User requester) {
        return ParticipationRequest.builder()
                .id(dto.getId())
                .created(LocalDateTime.parse(dto.getCreated(), FORMATTER))
                .event(event)
                .requester(requester)
                .status(dto.getStatus())
                .build();
    }

    public static List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        return requests.stream()
                .map(RequestsMapper::toDto)
                .collect(Collectors.toList());
    }
}
