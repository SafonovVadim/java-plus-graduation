package ru.practicum.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.StatsRepository;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.entity.EndpointHitEntity;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsRepository statsRepository;

    @Transactional
    public void saveHit(EndpointHit dto) {
        EndpointHitEntity entity = EndpointHitEntity.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
        log.info("Попадание {}", entity);
        statsRepository.save(entity);
    }

    @Transactional
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, Boolean unique) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end times must not be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        List<ViewStats> vs;
        if (Boolean.TRUE.equals(unique)) {
            vs = statsRepository.findUniqueStats(start, end, uris);
            log.info("Уникальные значения {}", vs);
        } else {
            vs = statsRepository.findAllStats(start, end, uris);
            log.info("Все значения {}", vs);
        }
        return vs;
    }
}
