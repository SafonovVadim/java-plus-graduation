package ru.practicum.service;

import ru.practicum.dto.users.UserDto;

public interface PublicUserService {

    UserDto findById(Long id);
}
