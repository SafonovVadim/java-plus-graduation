package ru.practicum.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.categories.Category;
import ru.practicum.categories.CategoryRepository;
import ru.practicum.events.dto.UpdateEventUserRequest;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.practicum.common.Constance.FORMATTER;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase
class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventsRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private Event event;
    private Category category;

    @BeforeEach
    void setUp() {
        // Создаём тестовую категорию
        category = Category.builder()
                .name("Test Category")
                .build();
        category = categoryRepository.save(category);

        // Создаём тестового пользователя
        user = User.builder()
                .name("Test User")
                .email("test@user.com")
                .build();
        user = userRepository.save(user);

        // Создаём тестовое событие в статусе PENDING
        event = Event.builder()
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
        event = eventRepository.save(event);
    }

    /**
     * Проверяет успешное обновление события с корректными данными → 200 OK.
     */
    @Test
    void shouldUpdateEventSuccessfully() throws Exception {
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .annotation("Updated annotation with sufficient length to meet the minimum 20 characters requirement")
                .title("Updated title that meets the minimum 3 characters requirement")
                .stateAction(StateAction.CANCEL_REVIEW)
                .build();

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", user.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId()))
                .andExpect(jsonPath("$.title").value("Updated title that meets the minimum 3 characters requirement"))
                .andExpect(jsonPath("$.annotation").value("Updated annotation with sufficient length to meet the minimum 20 characters requirement"))
                .andExpect(jsonPath("$.state").value("CANCELED"));
    }

    /**
     * Проверяет ошибку «событие не найдено» → 404 Not Found.
     */
    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        // Given
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .stateAction(StateAction.CANCEL_REVIEW)
                .build();

        // When & Then
        mockMvc.perform(patch("/users/{userId}/events/999", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("ID 999")));
    }

    /**
     * Проверяет ошибку «пользователь не инициатор» → 403 Forbidden.
     */
    @Test
    void shouldReturnForbiddenWhenUserIsNotInitiator() throws Exception {
        // Given: создаём другого пользователя
        User otherUser = User.builder().name("Other User").email("other@user.com").build();
        otherUser = userRepository.save(otherUser);

        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .stateAction(StateAction.CANCEL_REVIEW)
                .build();

        // When & Then
        mockMvc.perform(patch("/users/{userId}/events/{eventId}", otherUser.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("FORBIDDEN\",\"reason\":\"For the requested operation the conditions are not met.")));
    }

    /**
     * Проверяет ошибку «статус события не PENDING/CANCELED» → 403 Forbidden.
     */
    @Test
    void shouldReturnForbiddenWhenEventStateInvalid() throws Exception {
        // Given: обновляем событие до статуса PUBLISHED
        event.setState(EventState.PUBLISHED);
        eventRepository.save(event);

        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .stateAction(StateAction.CANCEL_REVIEW)
                .build();

        // When & Then
        mockMvc.perform(patch("/users/{userId}/events/{eventId}", user.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("status\":\"CONFLICT\",\"reason\":\"Conflict occurred.\",\"message")));
    }

    /**
     * Проверяет частичное обновление: обновляются только не‑null поля.
     */
    @Test
    void shouldPartiallyUpdateEvent() throws Exception {
        // Given: только аннотация, остальные поля null
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .annotation("Partially updated annotation")
                .stateAction(StateAction.CANCEL_REVIEW)
                .build();

        // When & Then
        mockMvc.perform(patch("/users/{userId}/events/{eventId}", user.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annotation").value("Partially updated annotation"))
                // Другие поля не должны измениться
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    /**
     * Проверяет ошибку при попытке обновить событие с несуществующей категорией.
     */
    @Test
    void shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
        // Given: указываем несуществующую категорию
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .category(999L)
                .stateAction(StateAction.CANCEL_REVIEW)
                .build();

        // When & Then
        mockMvc.perform(patch("/users/{userId}/events/{eventId}", user.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Category with id=999 was not found")));
    }

    /**
     * Проверяет обновление местоположения события.
     */
    @Test
    void shouldUpdateEventLocationSuccessfully() throws Exception {
        // Given
        Location newLocation = Location.builder()
                .lat(59.93f)
                .lon(30.34f)
                .build();

        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .location(newLocation)
                .stateAction(StateAction.CANCEL_REVIEW)
                .build();

        // When & Then
        mockMvc.perform(patch("/users/{userId}/events/{eventId}", user.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.lat").value(59.93f))
                .andExpect(jsonPath("$.location.lon").value(30.34f));
    }

    /**
     * Проверяет, что null‑поля в запросе не перезаписывают существующие значения.
     */
    @Test
    void shouldNotUpdateNullFields() throws Exception {
        // Given: только stateAction, все остальные поля null
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .stateAction(StateAction.CANCEL_REVIEW)
                .build();

        // When & Then
        mockMvc.perform(patch("/users/{userId}/events/{eventId}", user.getId(), event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                // Проверяем, что все исходные значения сохранились
                .andExpect(jsonPath("$.title").value("Test Event"))
                .andExpect(jsonPath("$.annotation").value("Test annotation"))
                .andExpect(jsonPath("$.eventDate").value(
                        Matchers.startsWith(event.getEventDate().format(FORMATTER))));
    }

    /**
     * Проверяет успешное получение событий пользователя с пагинацией → 200 OK.
     */
    @Test
    void shouldGetUserEventsWithPagination() throws Exception {
        // Given: создаём второе событие для того же пользователя
        Event secondEvent = Event.builder()
                .title("Second Test Event")
                .annotation("Second test annotation")
                .description("Second test description")
                .initiator(user)
                .state(EventState.PENDING)
                .eventDate(LocalDateTime.now().plusDays(3))
                .category(category)
                .paid(false)
                .participantLimit(20) // явно задаём, чтобы избежать ошибки БД
                .requestModeration(true)
                .locationLat(55.75f)
                .locationLon(37.62f)
                .confirmedRequests(10L)
                .createdOn(LocalDateTime.now())
                .views(0L)
                .build();
        eventRepository.save(secondEvent);

        // When & Then: запрашиваем события с пагинацией
        mockMvc.perform(get("/users/{userId}/events", user.getId())
                        .param("from", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(secondEvent.getId())) // id=27 (более свежая дата)
                .andExpect(jsonPath("$[0].title").value("Second Test Event"))
                .andExpect(jsonPath("$[1].id").value(event.getId()))   // id=26 (более старая дата)
                .andExpect(jsonPath("$[1].title").value("Test Event"));
    }

    /**
     * Проверяет получение пустого списка, если у пользователя нет событий → 200 OK с пустым массивом.
     */
    @Test
    void shouldReturnEmptyListWhenUserHasNoEvents() throws Exception {
        // Given: создаём пользователя без событий
        User userWithoutEvents = User.builder()
                .name("User Without Events")
                .email("noevents@user.com")
                .build();
        userRepository.save(userWithoutEvents);

        // When & Then
        mockMvc.perform(get("/users/{userId}/events", userWithoutEvents.getId())
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]")); // пустой массив
    }

    /**
     * Проверяет валидацию параметра 'from': отрицательное значение → 400 Bad Request.
     */
    @Test
    void shouldReturnBadRequestWhenFromIsNegative() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/{userId}/events", user.getId())
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверяет валидацию параметра 'size': ноль или отрицательное значение → 400 Bad Request.
     */
    @Test
    void shouldReturnBadRequestWhenSizeIsInvalid() throws Exception {
        // When & Then: size = 0
        mockMvc.perform(get("/users/{userId}/events", user.getId())
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        // When & Then: size = -5
        mockMvc.perform(get("/users/{userId}/events", user.getId())
                        .param("from", "0")
                        .param("size", "-5"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверяет использование значений по умолчанию для пагинации:
     * - без параметров from/size → 200 OK, возвращает 10 записей (size=10 по умолчанию);
     * - с параметрами from=0, size=10 → 200 OK, возвращает 10 записей.
     */
    @Test
    void shouldUseDefaultPaginationValues() throws Exception {
        // Given: создаём валидные события для проверки пагинации
        for (int i = 0; i < 15; i++) {
            Event additionalEvent = Event.builder()
                    .title("Event " + i)
                    .annotation("Annotation " + i)
                    .initiator(user)
                    .state(EventState.PENDING)
                    .eventDate(LocalDateTime.now().plusDays(i + 4))
                    .category(category)
                    .paid(false)
                    .participantLimit(10)
                    // Обязательные поля, которые были пропущены:
                    .createdOn(LocalDateTime.now())  // добавлено: createdOn
                    .requestModeration(true)        // добавлено: requestModeration
                    .confirmedRequests(0L)       // добавлено: confirmedRequests
                    .views(0L)                   // добавлено: views
                    .build();
            eventRepository.save(additionalEvent);
        }

        // When & Then: запрос без параметров (используются значения по умолчанию)
        mockMvc.perform(get("/users/{userId}/events", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10)); // по умолчанию size=10

        // When & Then: запрос с явными параметрами пагинации
        mockMvc.perform(get("/users/{userId}/events",user.getId())
                        .param("from", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].title").value("Event 14")) // самая свежая запись
                .andExpect(jsonPath("$[9].title").value("Event 5"));  // 10‑я запись
    }

    /**
     * Проверяет ошибку «пользователь не инициатор события» → 403 Forbidden.
     */
    @Test
    void shouldReturnForbiddenWhenUserIsNotEventInitiator() throws Exception {
        // Given: создаём другого пользователя и событие для него
        User otherUser = User.builder()
                .name("Other User")
                .email("other@user.com")
                .build();
        otherUser = userRepository.save(otherUser);

        Event otherEvent = Event.builder()
                .title("Other User's Event")
                .annotation("Annotation for other user")
                .description("Description for other user's event")
                .initiator(otherUser)
                .state(EventState.PENDING)
                .eventDate(LocalDateTime.now().plusDays(5))
                .category(category)
                .paid(false)
                .participantLimit(15)
                .requestModeration(true)
                .locationLat(59.93f)
                .locationLon(30.34f)
                .confirmedRequests(3L)
                .createdOn(LocalDateTime.now())
                .views(0L)
                .build();
        eventRepository.save(otherEvent);

        // When & Then: пытаемся получить событие другого пользователя
        mockMvc.perform(get("/users/{userId}/events/{eventId}", user.getId(), otherEvent.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.reason").value("For the requested operation the conditions are not met."))
                // Вариант 1: проверяем, что сообщение содержит подстроку (через matches)
                .andExpect(jsonPath("$.message").value(Matchers.containsString("не является инициатором")));
    }


    /**
     * Проверяет валидацию параметра userId: некорректный формат → 400 Bad Request.
     */
    @Test
    void shouldReturnBadRequestWhenUserIdIsInvalid() throws Exception {
        // When & Then: передаём строку вместо числа
        mockMvc.perform(get("/users/invalidUserId/events/1"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверяет валидацию параметра eventId: некорректный формат → 400 Bad Request.
     */
    @Test
    void shouldReturnBadRequestWhenEventIdIsInvalid() throws Exception {
        // When & Then: передаём строку вместо числа
        mockMvc.perform(get("/users/1/events/invalidEventId"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверяет получение события в статусе CANCELED.
     */
    @Test
    void shouldGetCancelledEvent() throws Exception {
        // Given: отменяем событие
        event.setState(EventState.CANCELED);
        eventRepository.save(event);

        // When & Then
        mockMvc.perform(get("/users/{userId}/events/{eventId}", user.getId(), event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELED"));
    }

    /**
     * Проверяет получение события с нулевым количеством подтверждённых заявок.
     */
    @Test
    void shouldGetEventWithZeroConfirmedRequests() throws Exception {
        // Given: сбрасываем подтверждённые заявки
        event.setConfirmedRequests(0L);
        eventRepository.save(event);

        // When & Then
        mockMvc.perform(get("/users/{userId}/events/{eventId}", user.getId(), event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests").value(0));
    }

}
