package ru.practicum.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.error.exception.ConflictException;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.events.Event;
import ru.practicum.events.EventState;
import ru.practicum.events.EventsRepository;
import ru.practicum.requests.ParticipationRequest;
import ru.practicum.requests.RequestRepository;
import ru.practicum.requests.participation.ParticipationsRequestsService;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationsRequestsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventsRepository eventsRepository;

    @Mock
    private RequestRepository requestRepository;

    @InjectMocks
    private ParticipationsRequestsService participationsRequestsService;

    @Test
    void createParticipationRequest_UserNotFound_ThrowsNotFoundException() {
        // Given
        Long userId = 999L;
        Long eventId = 100L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationsRequestsService.createParticipationRequest(userId, eventId));

        assertTrue(exception.getMessage().contains("User with id=" + userId));
    }

    @Test
    void createParticipationRequest_EventNotFound_ThrowsNotFoundException() {
        // Given
        Long userId = 1L;
        Long eventId = 999L;

        User requester = new User();
        requester.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(eventsRepository.findById(eventId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationsRequestsService.createParticipationRequest(userId, eventId));

        assertTrue(exception.getMessage().contains("Event with id=" + eventId));
    }

    @Test
    void createParticipationRequest_RequesterIsInitiator_ThrowsConflictException() {
        // Given
        Long userId = 1L;
        Long eventId = 100L;

        User requester = new User();
        requester.setId(userId);

        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);
        event.setInitiator(requester); // инициатор = запрашивающий

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(eventsRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> participationsRequestsService.createParticipationRequest(userId, eventId));

        assertEquals("User cannot request participation in their own event", exception.getMessage());
    }

    @Test
    void createParticipationRequest_UnpublishedEvent_ThrowsConflictException() {
        // Given
        Long userId = 1L;
        Long eventId = 100L;

        User requester = new User();
        requester.setId(userId);

        User initiator = new User(); // Создаём инициатора
        initiator.setId(2L); // ID отличается от requester

        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PENDING); // не PUBLISHED
        event.setInitiator(initiator); // Устанавливаем инициатора

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(eventsRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> participationsRequestsService.createParticipationRequest(userId, eventId));

        assertEquals("Cannot participate in unpublished event", exception.getMessage());
    }


    @Test
    void createParticipationRequest_DuplicateRequest_ThrowsConflictException() {
        // Given
        Long userId = 1L;
        Long eventId = 100L;

        User requester = new User();
        requester.setId(userId);

        User initiator = new User(); // Создаём инициатора
        initiator.setId(2L);

        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);
        event.setInitiator(initiator); // Устанавливаем инициатора

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(eventsRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(eventId, userId)).thenReturn(true); // дубликат

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> participationsRequestsService.createParticipationRequest(userId, eventId));

        assertEquals("Duplicate participation request", exception.getMessage());
    }


    @Test
    void createParticipationRequest_ParticipantLimitReached_ThrowsConflictException() {
        // Given
        Long userId = 1L;
        Long eventId = 100L;

        User requester = new User();
        requester.setId(userId);

        User initiator = new User(); // Создаём инициатора
        initiator.setId(2L);

        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);
        event.setParticipantLimit(5); // лимит 5 участников
        event.setRequestModeration(false);
        event.setInitiator(initiator); // Устанавливаем инициатора

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(eventsRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(eventId, userId)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(eventId, EventState.CONFIRMED)).thenReturn(5L); // уже 5 подтверждённых заявок

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> participationsRequestsService.createParticipationRequest(userId, eventId));

        assertEquals("Event participant limit reached", exception.getMessage());
    }

    @Test
    void cancelParticipationRequest_RequestNotFound_ThrowsNotFoundException() {
        // Given
        Long userId = 1L;
        Long requestId = 999L;

        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationsRequestsService.cancelParticipationRequest(userId, requestId));

        assertTrue(exception.getMessage().contains("Request with id=" + requestId + " was not found"));
    }

    @Test
    void cancelParticipationRequest_WrongUser_ThrowsNotFoundException() {
        // Given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long requestId = 1000L;

        User otherUser = new User();
        otherUser.setId(otherUserId);

        ParticipationRequest request = new ParticipationRequest();
        request.setId(requestId);
        request.setRequester(otherUser);
        request.setStatus(EventState.PENDING);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationsRequestsService.cancelParticipationRequest(userId, requestId));

        assertTrue(exception.getMessage().contains("is not accessible for user " + userId));
    }

    @Test
    void cancelParticipationRequest_AlreadyConfirmed_ThrowsIllegalArgumentException() {
        // Given
        Long userId = 1L;
        Long requestId = 1000L;

        User requester = new User();
        requester.setId(userId);

        ParticipationRequest request = new ParticipationRequest();
        request.setId(requestId);
        request.setRequester(requester);
        request.setStatus(EventState.CONFIRMED);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> participationsRequestsService.cancelParticipationRequest(userId, requestId));

        assertTrue(exception.getMessage().contains("Cannot cancel request with status: " + EventState.CONFIRMED));
    }

    @Test
    void cancelParticipationRequest_AlreadyCancelled_ThrowsIllegalArgumentException() {
        // Given
        Long userId = 1L;
        Long requestId = 1000L;

        User requester = new User();
        requester.setId(userId);

        ParticipationRequest request = new ParticipationRequest();
        request.setId(requestId);
        request.setRequester(requester);
        request.setStatus(EventState.CANCELED);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> participationsRequestsService.cancelParticipationRequest(userId, requestId));

        assertTrue(exception.getMessage().contains("Cannot cancel request with status: " + EventState.CANCELED));
    }

    @Test
    void getUserParticipationRequests_Success_EmptyList() {
        // Given
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(requestRepository.findByRequesterId(userId)).thenReturn(Collections.emptyList());

        // When
        List<ParticipationRequestDto> result = participationsRequestsService.getUserParticipationRequests(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findById(userId);
        verify(requestRepository, times(1)).findByRequesterId(userId);
    }

    @Test
    void getUserParticipationRequests_Success_WithRequests() {
        // Given
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        Event event1 = new Event();
        event1.setId(100L);

        Event event2 = new Event();
        event2.setId(200L);

        ParticipationRequest request1 = new ParticipationRequest();
        request1.setId(1000L);
        request1.setRequester(user);
        request1.setEvent(event1);
        request1.setStatus(EventState.PENDING);
        request1.setCreated(LocalDateTime.now().minusDays(1));

        ParticipationRequest request2 = new ParticipationRequest();
        request2.setId(1001L);
        request2.setRequester(user);
        request2.setEvent(event2);
        request2.setStatus(EventState.CONFIRMED);
        request2.setCreated(LocalDateTime.now());

        List<ParticipationRequest> requests = Arrays.asList(request1, request2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(requestRepository.findByRequesterId(userId)).thenReturn(requests);

        // When
        List<ParticipationRequestDto> result = participationsRequestsService.getUserParticipationRequests(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        ParticipationRequestDto dto1 = result.get(0);
        assertEquals(1000L, dto1.getId());
        assertEquals(EventState.PENDING, dto1.getStatus());
        assertEquals(100L, dto1.getEvent());

        ParticipationRequestDto dto2 = result.get(1);
        assertEquals(1001L, dto2.getId());
        assertEquals(EventState.CONFIRMED, dto2.getStatus());
        assertEquals(200L, dto2.getEvent());
    }

    @Test
    void getUserParticipationRequests_UserNotFound_ThrowsNotFoundException() {
        // Given
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationsRequestsService.getUserParticipationRequests(userId));

        assertTrue(exception.getMessage().contains("User with id=" + userId));
    }

}
