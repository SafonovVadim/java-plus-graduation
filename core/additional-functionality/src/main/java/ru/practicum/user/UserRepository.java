package ru.practicum.user;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
