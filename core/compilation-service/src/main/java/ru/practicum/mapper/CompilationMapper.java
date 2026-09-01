package ru.practicum.mapper;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.entity.Compilation;
import ru.practicum.feign.PublicEventsClient;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CompilationMapper {

    private final PublicEventsClient publicEventsClient;

    public CompilationDto toCompilation(Compilation compilation) {
        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setPinned(compilation.getPinned());
        dto.setTitle(compilation.getTitle());
        dto.setEvents(compilation.getEventIds().stream()
                .map(publicEventsClient::getEventShort)
                .collect(Collectors.toList()));
        return dto;
    }
}
