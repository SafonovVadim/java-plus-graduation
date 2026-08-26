package ru.practicum.service;

import ru.practicum.entity.User;

public interface PublicUserService {

    User findById(Long id);
}
