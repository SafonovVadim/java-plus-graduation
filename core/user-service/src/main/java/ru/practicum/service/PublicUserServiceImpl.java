package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.users.UserDto;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PublicUserServiceImpl implements PublicUserService {
    private final UserRepository userRepository;

    @Override
    public UserDto findById(Long id) {
        return UserMapper.toDto(userRepository.findById(id).orElseThrow(() -> new NotFoundException("Пользователь с id:" + id + " не существует")));
    }
}
