package ru.practicum;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.entity.Similarities;

public interface SimilaritiesRepository extends JpaRepository<Similarities, Long> {
}
