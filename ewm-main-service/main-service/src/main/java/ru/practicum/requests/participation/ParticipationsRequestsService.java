package ru.practicum.requests.participation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.error.exception.ConflictException;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.events.Event;
import ru.practicum.events.EventState;
import ru.practicum.events.EventsRepository;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.requests.ParticipationRequest;
import ru.practicum.requests.RequestRepository;
import ru.practicum.requests.RequestsMapper;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.common.Constance.FORMATTER;
import static ru.practicum.requests.RequestsMapper.toDto;

@AllArgsConstructor
@Service
@Slf4j
public class ParticipationsRequestsService {

    private UserRepository userRepository;
    private EventsRepository eventsRepository;
    private RequestRepository requestRepository;

    @Transactional
    public ParticipationRequestDto createParticipationRequest(Long userId, Long eventId) {
        // 1. Проверяем существование пользователя
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        // 2. Проверяем существование события
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        // 3. Проверяем, что пользователь не является инициатором события
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("User cannot request participation in their own event");
        }

        // 4. Проверяем статус события — должно быть PUBLISHED
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        // 5. Проверяем отсутствие дубликата заявки
        boolean hasExistingRequest = requestRepository.existsByEventIdAndRequesterId(eventId, userId);
        if (hasExistingRequest) {
            throw new ConflictException("Duplicate participation request");
        }

        // 6. Проверяем лимит заявок
        if (event.getParticipantLimit() > 0) {
            long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, EventState.CONFIRMED);
            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("Event participant limit reached");
            }
        }

        // 7. Создаём заявку
        ParticipationRequest request = new ParticipationRequest();
        request.setEvent(event);
        request.setRequester(requester);
        request.setCreated(LocalDateTime.now());
        log.info("Заявка при создании в методе {}", request);

        // 8. Устанавливаем статус с учётом лимита участников и настройки модерации
        if (event.getParticipantLimit() == 0) {
            request.setStatus(EventState.CONFIRMED);
            log.info("Автоподтверждение: лимит участников 0, статус установлен как CONFIRMED");
        } else if (Boolean.FALSE.equals(event.getRequestModeration())) {
            request.setStatus(EventState.CONFIRMED);
            log.info("Модерация отключена, статус установлен как CONFIRMED");
        } else {
            request.setStatus(EventState.PENDING);
            log.info("Требуется модерация, статус установлен как PENDING");
        }

        ParticipationRequest savedRequest = requestRepository.save(request);

        savedRequest.setRequester(requester);
        savedRequest.setEvent(event);
        log.debug("Дата создания в БД (после сохранения): {}\nСтроковое представление даты в DTO: {}",
                request.getCreated(), savedRequest.getCreated().format(FORMATTER));

        log.info("Создана заявка на участие с ID: {}, статус: {}", savedRequest.getId(), savedRequest.getStatus());

        return toDto(savedRequest);
    }

    @Transactional
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        // 1. Проверяем существование запроса
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        // 2. Проверяем, что запрос принадлежит пользователю
        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Request with id=" + requestId + " is not accessible for user " + userId);
        }

        // 3. Проверяем статус запроса — можно отменять только PENDING
        if (!EventState.PENDING.equals(request.getStatus())) {
            throw new IllegalArgumentException("Cannot cancel request with status: " + request.getStatus());
        }

        // 4. Обновляем статус на CANCELLED
        request.setStatus(EventState.CANCELED);

        ParticipationRequest savedRequest = requestRepository.save(request);

        log.debug("Дата создания в БД (до отмены): {}\nСтроковое представление даты в DTO после отмены: {}",
                request.getCreated(), toDto(savedRequest).getCreated());

        log.info("Заявка на участие с ID: {} отменена пользователем: {}", requestId, userId);
        return toDto(savedRequest);
    }

    public List<ParticipationRequestDto> getUserParticipationRequests(Long userId) {
        // 1. Проверяем существование пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        // 2. Получаем все заявки пользователя
        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);

        // 3. Преобразуем в DTO
        return requests.stream()
                .map(RequestsMapper::toDto)
                .collect(Collectors.toList());
    }
}
