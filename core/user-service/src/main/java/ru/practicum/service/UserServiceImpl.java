package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;
import ru.practicum.entity.User;
import ru.practicum.errors.exception.ConflictException;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.mapper.UserMapper.toDto;
import static ru.practicum.mapper.UserMapper.toEntity;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserDto save(NewUserRequest request) {
        log.info("Начинаем создание нового пользователя: {}", request.getName());

        // Проверяем уникальность email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with email " + request.getEmail() + " already exists");
        }

        User user = userRepository.save(toEntity(request));
        log.info("Пользователь успешно создан с ID: {}", user.getId());
        return toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> findByIdsOrAllWithPagination(List<Long> ids, int offset, int size) {
        List<User> users;
        log.debug("Получен запрос на получение пользователей. IDs: {}, offset: {}, size: {}", ids, offset, size);

        if (ids != null && !ids.isEmpty()) {
            // Возвращаем пользователей по массиву ids
            users = userRepository.findByIds(ids);
            log.debug("Найдено {} пользователей по указанным ID", users.size());
        } else {
            // Возвращаем пользователей с учетом пагинации
            users = userRepository.findAllWithOffset(offset, size);
            log.debug("Найдено {} пользователей без фильтрации по ID", users.size());
        }

        List<UserDto> result = users.stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());

        log.info("Возвращаем {} пользователей", result.size());
        return result;
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        log.info("Начинаем удаление пользователя с ID: {}", id);
        if (userRepository.deleteByIdAndReturnRow(id) == 0) {
            log.warn("Попытка удаления несуществующего пользователя с ID: {}", id);
            throw new NotFoundException("Пользователь с id:" + id + " не существует");
        }
        log.info("Пользователь с ID {} успешно удалён", id);
    }
}
