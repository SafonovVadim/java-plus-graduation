package ru.practicum.dto.requests;

import lombok.Builder;
import lombok.Data;
import ru.practicum.events.dto.EventState;

@Builder
@Data
public class ParticipationRequestDto {
    private String created;
    private Long event;
    private Long id;
    private Long requester;
    private EventState status;
}
