package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.users.UserDto;
import ru.practicum.feign.PublicUserClient;
import ru.practicum.service.PublicUserService;

@RestController
@RequestMapping("/public/users")
@RequiredArgsConstructor
public class PublicUserController implements PublicUserClient {
    private final PublicUserService userService;

    @Override
    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable Long userId) {
        return userService.findById(userId);
    }
}
