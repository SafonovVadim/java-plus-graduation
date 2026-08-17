package ru.practicum.requests.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.error.exception.ConflictException;
import ru.practicum.error.exception.ForbiddenActionException;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.events.Event;
import ru.practicum.events.EventState;
import ru.practicum.events.EventsRepository;
import ru.practicum.request.EventRequestStatusUpdateRequest;
import ru.practicum.request.EventRequestStatusUpdateResult;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.requests.ParticipationRequest;
import ru.practicum.requests.RequestRepository;
import ru.practicum.requests.RequestsMapper;
import ru.practicum.user.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RequestsServiceImpl implements RequestsService {

    private final EventsRepository eventsRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;

    @Override
    @SneakyThrows
    public EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request) {

        // 1. Проверяем существование события и принадлежность пользователю
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenActionException("User is not the initiator of the event");
        }

        // 2. Проверяем условия пре‑модерации и лимита (400 BAD_REQUEST)
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new BadRequestException("Request moderation is not required for this event");
        }

        // 3. Находим заявки для обновления
        List<ParticipationRequest> requests = requestRepository.findAllById(request.getRequestIds());
        if (requests.isEmpty()) {
            throw new NotFoundException("No requests found for the given IDs");
        }

        // 4. Проверяем, что все заявки в статусе PENDING (409 CONFLICT)
        boolean allPending = requests.stream()
                .allMatch(r -> r.getStatus() == EventState.PENDING);
        if (!allPending) {
            throw new ConflictException("All requests must be in PENDING status");
        }

        // 5. Проверяем лимит участников с учётом новых подтверждений
        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, EventState.CONFIRMED);
        int newConfirmedCount = (int) confirmedCount + request.getRequestIds().size();

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if (newConfirmedCount > event.getParticipantLimit()) {
            // 6. Автоматическое отклонение всех неподтверждённых заявок при исчерпании лимита
            List<ParticipationRequest> allPendingRequests = requestRepository
                    .findByEventIdAndStatus(eventId, EventState.PENDING);

            for (ParticipationRequest req : allPendingRequests) {
                req.setStatus(EventState.REJECTED);
                rejected.add(req);
            }
            requestRepository.saveAll(allPendingRequests);

            throw new ConflictException("The participant limit has been reached. All pending requests have been rejected.");
        } else {
            // 7. Обычное обновление статусов
            for (ParticipationRequest req : requests) {
                if (request.getStatus() == EventState.CONFIRMED) {
                    req.setStatus(EventState.CONFIRMED);
                    confirmed.add(req);
                } else if (request.getStatus() == EventState.REJECTED) {
                    req.setStatus(EventState.REJECTED);
                    rejected.add(req);
                }
            }
            requestRepository.saveAll(requests);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(RequestsMapper.toDtoList(confirmed))
                .rejectedRequests(RequestsMapper.toDtoList(rejected))
                .build();
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        // 1. Проверяем существование события и принадлежность пользователю
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenActionException("User is not the initiator of the event");
        }

        // 2. Получаем все заявки на событие
        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        // 3. Преобразуем в DTO
        return RequestsMapper.toDtoList(requests);
    }
}
