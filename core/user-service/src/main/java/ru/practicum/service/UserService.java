package ru.practicum.service;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;
import ru.practicum.entity.User;

import java.util.List;

public interface UserService {
    UserDto save(NewUserRequest request);

    List<UserDto> findByIdsOrAllWithPagination(List<Long> ids, int offset, int size);

    @Transactional
    void deleteById(Long id);

}
