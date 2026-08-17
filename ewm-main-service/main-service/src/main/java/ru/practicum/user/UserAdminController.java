package ru.practicum.user;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@Slf4j
@RequiredArgsConstructor
@Validated
public class UserAdminController {

    private final UserService userService;

    /**
     * Создаёт нового пользователя через административный API.
     *
     * @param request DTO с данными нового пользователя (имя и email).
     *               Обязательные поля:
     *               - name: не должно быть пустым, длина от 1 до 255 символов
     *               - email: должен быть корректным email-адресом
     * @return ResponseEntity с UserDto и HTTP‑статусом 201 (CREATED)
     * @throws MethodArgumentNotValidException если данные не прошли валидацию
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public UserDto createUser(@Valid @RequestBody NewUserRequest request) {
        log.info("Начата обработка запроса на создание пользователя");
        log.debug("Получены данные для создания пользователя: name='{}', email='{}'",
                request.getName(), request.getEmail());

        UserDto dto = userService.save(request);

        log.info("Пользователь успешно создан с ID: {}", dto.getId());
        log.debug("Данные созданного пользователя: {}", dto);
        return dto;
    }

    /**
     * Получает список пользователей по заданным критериям.
     *
     * @param ids необязательный список ID пользователей для фильтрации.
     *            Если не указан — возвращаются все пользователи.
     * @param offset индекс начала выборки (нумерация с 0).
     *             Минимальное значение: 0.
     * @param size размер страницы результатов.
     *            Минимальное значение: 1.
     * @return список UserDto, соответствующий критериям поиска
     * @throws ConstraintViolationException если параметры не прошли валидацию
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<UserDto> get(
            @RequestParam(name = "ids", required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        log.info("Начата обработка запроса на получение списка пользователей");
        log.debug("Параметры запроса: ids={}, offset={}, size={}",
                ids != null ? String.join(",", ids.toString()) : "null", offset, size);

        List<UserDto> users = userService.findByIdsOrAllWithPagination(ids, offset, size);

        log.info("Получено {} пользователей (от {}, размер страницы {})",
                users.size(), offset, size);
        log.debug("Список полученных пользователей: {}", users);
        return users;
    }

    /**
     * Удаляет пользователя по его ID.
     *
     * @param userId уникальный идентификатор пользователя.
     *               Должен быть положительным числом (> 0)
     * @throws NotFoundException если пользователь с указанным ID не найден
     * @throws ConstraintViolationException если ID не прошёл валидацию
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("{userId}")
    public void delete(@PathVariable @Positive Long userId) {
        log.info("Начата обработка запроса на удаление пользователя с ID: {}", userId);

        userService.deleteById(userId);

        log.info("Пользователь с ID {} успешно удалён", userId);
    }
}
