package ru.practicum.compilations.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.entity.Compilation;
import ru.practicum.repository.CompilationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Compilation> page;
        if (pinned != null) {
            page = compilationRepository.findByPinned(pinned, pageable);
        } else {
            page = compilationRepository.findAll(pageable);
        }
        return page.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new RuntimeException("Compilation not found with id: " + compId));
        return toDto(compilation);
    }

    private CompilationDto toDto(Compilation compilation) {
        List<EventShortDto> events = null;
        if (compilation.getEvents() != null) {
            events = compilation.getEvents().stream()
                    .map(event -> {
                        EventShortDto dto = new EventShortDto();
                        dto.setId(event.getId());
                        dto.setTitle(event.getTitle());
                        return dto;
                    })
                    .collect(Collectors.toList());
        }
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(events)
                .build();
    }
}
