package ru.practicum.service;

import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;
import ru.practicum.entity.User;

import java.util.List;

public interface UserService {
    UserDto save(NewUserRequest request);

    List<UserDto> findByIdsOrAllWithPagination(List<Long> ids, int offset, int size);

    void deleteById(Long id);

    User findById(Long id);
}
