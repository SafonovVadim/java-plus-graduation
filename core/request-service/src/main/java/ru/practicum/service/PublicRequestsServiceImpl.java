package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.repository.RequestRepository;

@Service
@RequiredArgsConstructor
public class PublicRequestsServiceImpl implements PublicRequestsService {
    private final RequestRepository requestRepository;

    @Override
    public long countByEventIdAndStatus(long eventId, ru.practicum.events.dto.EventState status) {
        return requestRepository.countByEventIdAndStatus(eventId, status);
    }
}
