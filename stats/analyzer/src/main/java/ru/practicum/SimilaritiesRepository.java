package ru.practicum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.entity.Similarities;

import java.util.List;
import java.util.Optional;

public interface SimilaritiesRepository extends JpaRepository<Similarities, Long> {
    Optional<Similarities> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("SELECT sim FROM Similarities sim WHERE sim.eventA = :eventId OR sim.eventB = :eventId ORDER BY sim.score DESC")
    List<Similarities> findByEventId(@Param("eventId") Long eventId);
}
