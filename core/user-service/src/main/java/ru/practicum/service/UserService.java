package ru.practicum.service;

import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;

import java.util.List;

public interface UserService {
    UserDto save(NewUserRequest request);

    List<UserDto> findByIdsOrAllWithPagination(List<Long> ids, int offset, int size);

    void deleteById(Long id);
}
