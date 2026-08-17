package ru.practicum.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static String pattern = "yyyy-MM-dd HH:mm:ss";
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

    /**
     * Тест 1: Успешное создание пользователя (201 Created)
     */
    @Test
    void createUser_Success() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("ivan.petrov@practicummail.ru");
        request.setName("Иван Петров");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ivan.petrov@practicummail.ru"))
                .andExpect(jsonPath("$.name").value("Иван Петров"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    /**
     * Тест 2: Дублирование email (409 Conflict)
     */
    @Test
    void createUser_DuplicateEmail_Conflict() throws Exception {
        // Первый запрос — успешный (создаём пользователя)
        NewUserRequest firstRequest = new NewUserRequest();
        firstRequest.setEmail("duplicate@example.com");
        firstRequest.setName("Duplicate User");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(firstRequest)))
                .andExpect(status().isCreated());

        // Второй запрос с тем же email — должен вызвать ошибку 409
        NewUserRequest secondRequest = new NewUserRequest();
        secondRequest.setEmail("duplicate@example.com");
        secondRequest.setName("Another User");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason").value("Conflict occurred."))
                .andExpect(jsonPath("$.message").value(containsString("User with email duplicate@example.com already exists")))
                .andExpect(isValidTimestampFormat());
    }

    /**
     * Тест 3: Некорректный запрос (400 Bad Request)
     * Проверяем валидацию: пустой email и null name
     */
    @Test
    void createUser_InvalidRequest_BadRequest() throws Exception {
        NewUserRequest invalidRequest = new NewUserRequest();
        invalidRequest.setEmail("");
        invalidRequest.setName(null);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value(containsString("Field: name")))
                .andExpect(jsonPath("$.message").value(containsString("Имя не может быть пустым")))
                .andExpect(jsonPath("$.message").value(containsString("Field: email")))
                .andExpect(jsonPath("$.message").value(containsString("Email не может быть пустым")))
                .andExpect(isValidTimestampFormat());
    }

    /**
     * Тест 4: Успешное удаление пользователя (204 No Content)
     */
    @Test
    void deleteUser_Success() throws Exception {
        // Сначала создаём пользователя
        NewUserRequest createRequest = new NewUserRequest();
        createRequest.setEmail("delete.user@practicummail.ru");
        createRequest.setName("Delete User");

        ResultActions createResult = mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("delete.user@practicummail.ru"))
                .andExpect(jsonPath("$.name").value("Delete User"))
                .andExpect(jsonPath("$.id").isNumber());

        // Извлекаем ID созданного пользователя
        MvcResult createMvcResult = createResult.andReturn();
        String responseContent = createMvcResult.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseContent);
        Long userId = response.get("id").asLong();

        // Теперь удаляем созданного пользователя
        mockMvc.perform(delete("/admin/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                // Тело ответа должно быть пустым для 204 No Content
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().isEmpty()));
    }

    /**
     * Тест 5: Удаление несуществующего пользователя (404 Not Found)
     */
    @Test
    void deleteUser_UserNotFound_404() throws Exception {
        Long nonExistentUserId = 999999L;

        mockMvc.perform(delete("/admin/users/{userId}", nonExistentUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value(containsString("Пользователь с id:" + nonExistentUserId + " не существует")))
                .andExpect(isValidTimestampFormat());
    }

    /**
     * Тест 6: Удаление с некорректным ID (отрицательный) — 400 Bad Request
     */
    @Test
    void deleteUser_InvalidNegativeId_BadRequest() throws Exception {
        Long negativeUserId = -1L;

        mockMvc.perform(delete("/admin/users/{userId}", negativeUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value(containsString("must be greater than 0")))
                .andExpect(isValidTimestampFormat());
    }

    /**
     * Тест 7: Получение всех пользователей без параметров (по умолчанию offset=0, size=10)
     */
    @Test
    void getUsers_DefaultParams_Success() throws Exception {
        // Создаём несколько пользователей для тестирования
        createTestUsers(15); // создаём 15 пользователей

        // Запрос без параметров — должны вернуться первые 10 пользователей (from=0, size=10)
        mockMvc.perform(get("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10)) // должно быть 10 элементов
                .andExpect(jsonPath("$[0].id").value(1)) // первый пользователь имеет id=1
                .andExpect(jsonPath("$[9].id").value(10)); // последний в списке имеет id=10
    }

    /**
     * Тест 8: Получение пользователей с указанием offset и size
     */
    @Test
    void getUsers_WithFromAndSize_Success() throws Exception {
        createTestUsers(25); // создаём 25 пользователей

        // Запрос: пропустить 10 элементов, вернуть следующие 5
        mockMvc.perform(get("/admin/users?offset=10&size=5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5)) // должно быть 5 элементов
                .andExpect(jsonPath("$[0].id").value(11)) // первый в списке имеет id=11
                .andExpect(jsonPath("$[4].id").value(15)); // последний имеет id=15
    }

    /**
     * Тест 9: Получение пользователей по ID (с параметром ids)
     */
    @Test
    void getUsers_ByIds_Success() throws Exception {
        // Создаём пользователей и запоминаем их ID
        List<Long> expectedIds = createTestUsersWithIds(5); // создаём 5 пользователей, получаем их ID

        // Формируем строку с ID для параметра запроса
        String idsParam = String.join(",", expectedIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new));

        // Запрос только по указанным ID
        MvcResult result = mockMvc.perform(get("/admin/users?ids=" + idsParam)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(expectedIds.size())) // количество совпадает с переданным
                .andReturn(); // получаем результат выполнения запроса

        // Извлекаем содержимое ответа
        String responseContent = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseContent);

        // Проверяем, что все возвращённые ID есть в запросе
        for (int i = 0; i < response.size(); i++) {
            Long returnedId = response.get(i).get("id").asLong();
            assertTrue(expectedIds.contains(returnedId));
        }
    }

    /**
     * Тест 10: Получение пользователей по ID (без пагинации)
     */
    @Test
    void getUsers_ByIdsWithoutPagination_Success() throws Exception {
        List<Long> allIds = createTestUsersWithIds(20); // создаём 20 пользователей
        List<Long> targetIds = allIds.subList(0, 15); // берём первые 15 ID для фильтрации

        String idsParam = String.join(",", targetIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new));

        // Теперь не ожидаем пагинацию — должны получить всех 15 пользователей
        mockMvc.perform(get("/admin/users?ids=" + idsParam + "&offset=5&size=3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15)) // Ожидаем 15 записей
                .andExpect(jsonPath("$[0].id").value(targetIds.get(0))) // Первый ID из targetIds
                .andExpect(jsonPath("$[14].id").value(targetIds.get(14))); // Последний ID из targetIds
    }

    /**
     * Тест 11: Некорректный параметр from (отрицательный) — 400 Bad Request
     */
    @Test
    void getUsers_InvalidNegativeFrom_BadRequest() throws Exception {
        mockMvc.perform(get("/admin/users?offset=-1&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value(containsString("must be greater than or equal to 0")))
                .andExpect(isValidTimestampFormat());
    }

    /**
     * Тест 12: Некорректный параметр size (отрицательный) — 400 Bad Request
     */
    @Test
    void getUsers_InvalidNegativeSize_BadRequest() throws Exception {
        mockMvc.perform(get("/admin/users?offset=0&size=-5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value(containsString("must be greater than or equal to 1")))
                .andExpect(isValidTimestampFormat());
    }


    /**
     * Вспомогательный метод для сериализации объектов в JSON
     */
    private String asJsonString(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Вспомогательный метод проверки формата LocalDateTime (yyyy-MM-dd HH:mm:ss) в теле ответа
     *
     * @return
     */
    private ResultMatcher isValidTimestampFormat() {
        return result -> {
            try {
                String responseContent = result.getResponse().getContentAsString();
                JsonNode response = objectMapper.readTree(responseContent);
                String timestamp = response.get("timestamp").asText();

                if (timestamp == null || timestamp.isEmpty()) {
                    fail("Поле \"timestamp\" отсутствует или пусто в ответе");
                }

                assertDoesNotThrow(
                        () -> LocalDateTime.parse(timestamp, formatter),
                        "Формат timestamp не соответствует шаблону " + pattern + ". Значение: " + timestamp
                );
            } catch (Exception e) {
                fail("Ошибка при проверке формата timestamp: " + e.getMessage());
            }
        };
    }

    /**
     * Вспомогательный метод: создаёт тестовых пользователей и возвращает их ID
     */
    private List<Long> createTestUsersWithIds(int count) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            NewUserRequest request = new NewUserRequest();
            request.setEmail("test" + i + "@example.com");
            request.setName("Test User " + i);

            ResultActions result = mockMvc.perform(post("/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated());

            MvcResult mvcResult = result.andReturn();
            String responseContent = mvcResult.getResponse().getContentAsString();
            JsonNode response = objectMapper.readTree(responseContent);
            ids.add(response.get("id").asLong());
        }
        return ids;
    }

    /**
     * Вспомогательный метод: создаёт тестовых пользователей (без возврата ID)
     */
    private void createTestUsers(int count) throws Exception {
        for (int i = 1; i <= count; i++) {
            NewUserRequest request = new NewUserRequest();
            request.setEmail("test" + i + "@example.com");
            request.setName("Test User " + i);

            mockMvc.perform(post("/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated());
        }
    }

}
