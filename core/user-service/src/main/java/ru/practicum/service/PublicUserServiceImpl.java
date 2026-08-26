package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.entity.User;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PublicUserServiceImpl implements PublicUserService {
    private final UserRepository userRepository;

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Пользователь с id:" + id + " не существует"));
    }
}
