package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import ru.practicum.RequestRepository;
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.entity.Event;
import ru.practicum.entity.ParticipationRequest;
import ru.practicum.errors.exception.ConflictException;
import ru.practicum.errors.exception.ForbiddenActionException;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.feign.PrivateEventsClient;
import ru.practicum.feign.PublicEventsClient;
import ru.practicum.feign.PublicUserClient;
import ru.practicum.mapper.EventsMapper;
import ru.practicum.mapper.RequestsMapper;

import java.util.ArrayList;
import java.util.List;

import static ru.practicum.mapper.RequestsMapper.toDto;

@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final PublicEventsClient publicEventsClient;
    private final PrivateEventsClient privateEventsClient;
    private final PublicUserClient publicUserClient;
    private final RequestRepository requestRepository;

    @Override
    @SneakyThrows
    public EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
        Event event = EventsMapper.toEvent(privateEventsClient.getUserEventById(eventId));
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Только инициатор события может изменять статусы заявок");
        }
        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);
        ru.practicum.events.dto.EventState newStatus = request.getStatus();

        if (requests.size() != request.getRequestIds().size()) {
            throw new NotFoundException("Один или несколько запросов не найдены");
        }

        for (ParticipationRequest r : requests) {
            if (r.getStatus() != ru.practicum.events.dto.EventState.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок в состоянии PENDING");
            }
            if (!r.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Заявка относится к другому событию");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (newStatus == ru.practicum.events.dto.EventState.CONFIRMED) {
            Integer limit = event.getParticipantLimit();
            long available = (limit == null || limit == 0) ? Integer.MAX_VALUE : (limit - requestRepository.countByEventIdAndStatus(eventId, ru.practicum.events.dto.EventState.CONFIRMED));

            if (available <= 0) {
                throw new ConflictException("Достигнут лимит участников");
            }

            long toConfirm = Math.min(available, requests.size());
            for (int i = 0; i < requests.size(); i++) {
                ParticipationRequest req = requests.get(i);
                if (i < toConfirm) {
                    req.setStatus(ru.practicum.events.dto.EventState.CONFIRMED);
                    confirmed.add(toDto(req));
                } else {
                    req.setStatus(ru.practicum.events.dto.EventState.REJECTED);
                    rejected.add(toDto(req));
                }
            }
        } else {
            for (ParticipationRequest req : requests) {
                req.setStatus(ru.practicum.events.dto.EventState.REJECTED);
                rejected.add(toDto(req));
            }
        }

        requestRepository.saveAll(requests);
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);
        return result;
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        // 1. Проверяем существование события и принадлежность пользователю
        Event event = EventsMapper.toEvent(privateEventsClient.getUserEventById(eventId));

        if (!event.getInitiatorId().equals(userId)) {
            throw new ForbiddenActionException("User is not the initiator of the event");
        }

        // 2. Получаем все заявки на событие
        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        // 3. Преобразуем в DTO
        return RequestsMapper.toDtoList(requests);
    }
}
