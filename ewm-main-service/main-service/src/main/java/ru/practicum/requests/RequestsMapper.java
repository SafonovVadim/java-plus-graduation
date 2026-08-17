package ru.practicum.requests;

import org.springframework.stereotype.Component;
import ru.practicum.request.ParticipationRequestDto;

import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.common.Constance.FORMATTER;

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

    public static List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        return requests.stream()
                .map(RequestsMapper::toDto)
                .collect(Collectors.toList());
    }
}
