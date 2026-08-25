package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.entity.User;
import ru.practicum.feign.PublicUserClient;
import ru.practicum.user.service.PublicUserService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements PublicUserClient {
    private final PublicUserService userService;

    @Override
    public User getUser(@PathVariable Long userId) {
        return userService.getUser(userId);
    }
}
