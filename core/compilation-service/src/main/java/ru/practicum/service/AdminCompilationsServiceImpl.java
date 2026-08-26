package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.compilations.NewCompilationDto;
import ru.practicum.dto.compilations.UpdateCompilationRequest;
import ru.practicum.entity.Compilation;
import ru.practicum.errors.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.CompilationRepository;

import java.util.LinkedHashSet;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminCompilationsServiceImpl implements AdminCompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(Boolean.TRUE.equals(dto.getPinned()));
        compilation.setEventIds(dto.getEvents() != null ? new LinkedHashSet<>(dto.getEvents()) : new LinkedHashSet<>());

        Compilation saved = compilationRepository.save(compilation);
        Compilation detailed = compilationRepository.findDetailedById(saved.getId())
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + saved.getId() + " was not found"));
        return compilationMapper.toCompilation(detailed);
    }

    @Transactional
    @Override
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с ID: {}", compId);
        long deletedRows = compilationRepository.deleteCompilationById(compId);
        if (deletedRows == 0) throw new NotFoundException("Compilation with id=" + compId + " was not found");
    }

    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (dto.getEvents() != null) {
            compilation.setEventIds(new LinkedHashSet<>(dto.getEvents()));
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }

        compilationRepository.save(compilation);
        Compilation detailed = compilationRepository.findDetailedById(compilation.getId())
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compilation.getId() + " was not found"));
        return compilationMapper.toCompilation(detailed);
    }
}
