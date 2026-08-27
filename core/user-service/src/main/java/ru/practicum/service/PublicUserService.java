package ru.practicum.service;

import ru.practicum.dto.users.UserDto;
import ru.practicum.entity.User;

public interface PublicUserService {

    UserDto findById(Long id);
}
