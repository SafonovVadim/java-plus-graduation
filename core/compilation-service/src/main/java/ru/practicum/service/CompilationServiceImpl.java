package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.entity.Compilation;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.CompilationRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        List<Long> compilationIds = compilationRepository.findByPinned(pinned, PageRequest.of(from / size, size))
                .getContent();
        if (compilationIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> orderById = new LinkedHashMap<>();
        for (int index = 0; index < compilationIds.size(); index++) {
            orderById.put(compilationIds.get(index), index);
        }

        return compilationRepository.findAllDetailedByIdIn(compilationIds).stream()
                .sorted(Comparator.comparingInt(c -> orderById.getOrDefault(c.getId(), Integer.MAX_VALUE)))
                .map(compilationMapper::toCompilation)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation detailed = compilationRepository.findDetailedById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        return compilationMapper.toCompilation(detailed);
    }
}
