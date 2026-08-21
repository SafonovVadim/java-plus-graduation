package ru.practicum.requests.dto;

import lombok.Data;
import ru.practicum.events.dto.EventState;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds;
    private EventState status;
}

