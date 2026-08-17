package ru.practicum.events;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.categories.Category;
import ru.practicum.categories.CategoryDto;
import ru.practicum.categories.CategoryRepository;
import ru.practicum.error.exception.ForbiddenActionException;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.service.EventsService;
import ru.practicum.requests.RequestRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;
import ru.practicum.user.UserShortDto;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.practicum.common.Constance.FORMATTER;

@SpringBootTest
@AutoConfigureMockMvc
class PrivateEventControllerMockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventsService eventsService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private EventsRepository eventRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private RequestRepository requestRepository;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_EVENT_ID = 2L;

    @Test
    void shouldGetUserEventByIdSuccessfully() throws Exception {
        // Given: создаём тестовые данные
        Category category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();

        User user = User.builder()
                .id(TEST_USER_ID)
                .name("Test User")
                .email("test@user.com")
                .build();

        Event event = Event.builder()
                .id(TEST_EVENT_ID)
                .title("Test Event")
                .annotation("Test annotation")
                .description("Test description")
                .initiator(user)
                .state(EventState.PENDING)
                .eventDate(LocalDateTime.now().plusDays(2))
                .category(category)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .locationLat(55.75f)
                .locationLon(37.62f)
                .confirmedRequests(5L)
                .createdOn(LocalDateTime.now())
                .views(0L)
                .build();

        CategoryDto categoryDto = CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();

        UserShortDto userShortDto = UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();

        Location location = Location.builder()
                .lat(55.75F)
                .lon(37.62F)
                .build();

        EventFullDto expectedDto = EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryDto)
                .confirmedRequests(5L)
                .createdOn(event.getCreatedOn().toString())
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(FORMATTER))
                .initiator(userShortDto)
                .location(location)
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() != null ? event.getPublishedOn().toString() : null)
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .title(event.getTitle())
                .views(event.getViews())
                .build();

        // When: настраиваем моки
        when(eventsService.getUserEventById(TEST_USER_ID, TEST_EVENT_ID))
                .thenReturn(expectedDto);

        // Then: выполняем запрос и проверяем результат
        mockMvc.perform(get("/users/{userId}/events/{eventId}", TEST_USER_ID, TEST_EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_EVENT_ID))
                .andExpect(jsonPath("$.title").value("Test Event"))
                .andExpect(jsonPath("$.annotation").value("Test annotation"))
                .andExpect(jsonPath("$.description").value("Test description"))
                .andExpect(jsonPath("$.category.id").value(1L))
                .andExpect(jsonPath("$.category.name").value("Test Category"))
                .andExpect(jsonPath("$.confirmedRequests").value(5L))
                .andExpect(jsonPath("$.createdOn").value(Matchers.startsWith(LocalDateTime.now().toString().substring(0, 7))))
                .andExpect(jsonPath("$.eventDate").value(Matchers.startsWith(event.getEventDate().toString().substring(0, 7))))
                .andExpect(jsonPath("$.initiator.id").value(TEST_USER_ID))
                .andExpect(jsonPath("$.initiator.name").value("Test User"))
                .andExpect(jsonPath("$.location.lat").value(55.75))
                .andExpect(jsonPath("$.location.lon").value(37.62))
                .andExpect(jsonPath("$.paid").value(false))
                .andExpect(jsonPath("$.participantLimit").value(10))
                .andExpect(jsonPath("$.requestModeration").value(true))
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andExpect(jsonPath("$.views").value(0L));
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        // Given
        when(eventsService.getUserEventById(TEST_USER_ID, TEST_EVENT_ID))
                .thenThrow(new NotFoundException("Event with id=" + TEST_EVENT_ID + " was not found"));

        // When & Then
        mockMvc.perform(get("/users/{userId}/events/{eventId}", TEST_USER_ID, TEST_EVENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Event with id=2 was not found")));
    }

    @Test
    void shouldReturnForbiddenWhenUserIsNotInitiator() throws Exception {
        // Given
        when(eventsService.getUserEventById(TEST_USER_ID, TEST_EVENT_ID))
                .thenThrow(new ForbiddenActionException(
                        "Пользователь с ID " + TEST_USER_ID + " не является инициатором события " + TEST_EVENT_ID
                ));

        // When & Then
        mockMvc.perform(get("/users/{userId}/events/{eventId}", TEST_USER_ID, TEST_EVENT_ID))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("reason\":\"For the requested operation the conditions are not met")));
    }

    @Test
    void shouldThrowForbiddenWhenUserIsNotInitiator() throws Exception {
        Long userId = 1L;
        Long eventId = 2L;

        String expectedMessage = "Пользователь с ID " + userId + " не является инициатором события " + eventId;

        // Настраиваем мок сервиса на выброс ForbiddenActionException
        when(eventsService.getUserEventById(userId, eventId))
                .thenThrow(new ForbiddenActionException(expectedMessage));

        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, eventId))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString(" requested operation the conditions are not met.\",\"mess")));
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() throws Exception {
        Long userId = 999L;
        Long eventId = 2L;

        // Сервис должен выбросить NotFoundException при отсутствии пользователя
        when(eventsService.getUserEventById(userId, eventId))
                .thenThrow(new NotFoundException("Пользователь с ID " + userId + " не найден"));

        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, eventId))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("e required object was not found.\",\"mess")));
    }

    @Test
    void shouldCountConfirmedRequestsCorrectly() throws Exception {
        Long userId = 1L;
        Long eventId = 2L;

        User user = User.builder().id(userId).build();
        Event event = Event.builder().id(eventId).initiator(user).build();

        // Мокаем сервис, чтобы он вернул событие с нужным количеством подтверждённых заявок
        when(eventsService.getUserEventById(userId, eventId)).thenReturn(
                EventFullDto.builder()
                        .id(eventId)
                        .confirmedRequests(3L)
                        .build()
        );

        // Выполняем запрос
        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests").value(3L));

        // Проверяем, что сервис был вызван
        verify(eventsService, times(1)).getUserEventById(userId, eventId);
    }

    @Test
    void shouldUpdateViewsOnEventAccess() throws Exception {
        Long userId = 1L;
        Long eventId = 2L;

        User user = User.builder().id(userId).build();
        Event event = Event.builder().id(eventId).initiator(user).views(5L).build();

        // Мокаем сервис: возвращаем событие с обновлённым количеством просмотров
        EventFullDto expectedDto = EventFullDto.builder()
                .id(eventId)
                .views(6L) // +1 просмотр
                .build();

        when(eventsService.getUserEventById(userId, eventId)).thenReturn(expectedDto);

        // Выполняем запрос
        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views").value(6L));

        // Проверяем, что сервис был вызван один раз
        verify(eventsService, times(1)).getUserEventById(userId, eventId);
    }

}
