package ru.practicum.user.service;

import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.user.NewUserRequest;
import ru.practicum.user.UserDto;
import ru.practicum.error.exception.NotFoundException;

import java.util.List;

/**
 * Сервис для управления пользователями в системе.
 */
public interface UserService {

    /**
     * Создаёт нового пользователя в системе.
     *
     * @param request DTO с данными нового пользователя для создания.
     *               Обязательные поля:
     *               - name: не должно быть пустым, длина от 1 до 255 символов
     *               - email: должен быть корректным email‑адресом и уникальным
     * @return DTO созданного пользователя с заполненным ID
     * @throws DataIntegrityViolationException если email уже существует в системе
     * (конфликт уникальности будет обработан в GlobalExceptionHandler)
     */
    UserDto save(NewUserRequest request);

    /**
     * Получает список пользователей с поддержкой фильтрации и пагинации.
     *
     * @param ids необязательный список ID пользователей для фильтрации.
     *            Если null или пустой — возвращаются все пользователи.
     * @param offset количество элементов для пропуска (OFFSET в SQL).
     *            Минимальное значение: 0.
     * @param size количество элементов в наборе (LIMIT в SQL).
     *            Минимальное значение: 1.
     * @return список DTO пользователей, соответствующий критериям поиска.
     *         Может быть пустым, если пользователей не найдено.
     * @throws IllegalArgumentException если параметры пагинации некорректны
     */
    List<UserDto> findByIdsOrAllWithPagination(List<Long> ids, int offset, int size);

    /**
     * Удаляет пользователя по его уникальному идентификатору.
     *
     * @param id уникальный идентификатор пользователя для удаления.
     *           Должен быть положительным числом (> 0).
     * @throws NotFoundException если пользователь с указанным ID не найден в системе
     * @throws IllegalArgumentException если ID некорректен
     */
    void deleteById(Long id);
}
