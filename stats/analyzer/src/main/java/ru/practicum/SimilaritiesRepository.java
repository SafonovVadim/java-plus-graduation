package ru.practicum;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.entity.Similarities;

import java.util.Optional;

public interface SimilaritiesRepository extends JpaRepository<Similarities, Long> {
    Optional<Similarities> findByEventAAndEventB(Long eventA, Long eventB);
}
