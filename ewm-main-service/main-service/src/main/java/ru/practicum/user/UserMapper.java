package ru.practicum.user;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(NewUserRequest request) {
        return request == null ? null
                : new User(request.getName(), request.getEmail());
    }

    public UserDto toDto(User user) {
        return user == null ? null
                : new UserDto(user.getId(),  user.getName(), user.getEmail());
    }

    public UserShortDto toShortDto(User user) {
        return user == null ? null
                : new UserShortDto(user.getId(), user.getName());
    }
}

