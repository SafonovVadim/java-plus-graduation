package ru.practicum.request;

import lombok.Data;
import ru.practicum.events.EventState;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds;
    private EventState status;
}

