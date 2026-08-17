package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.error.exception.ConflictException;
import ru.practicum.user.NewUserRequest;
import ru.practicum.user.UserDto;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.user.User;
import ru.practicum.user.UserMapper;
import ru.practicum.user.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserServiceImp implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;


    @Override
    public UserDto save(NewUserRequest request) {
        log.info("Начинаем создание нового пользователя: {}", request.getName());

        // Проверяем уникальность email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with email " + request.getEmail() + " already exists");
        }

        User user = userRepository.save(userMapper.toEntity(request));
        log.info("Пользователь успешно создан с ID: {}", user.getId());
        return userMapper.toDto(user);
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
                .map(userMapper::toDto)
                .collect(Collectors.toList());

        log.info("Возвращаем {} пользователей", result.size());
        return result;
    }

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
