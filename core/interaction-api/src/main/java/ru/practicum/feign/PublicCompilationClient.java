package ru.practicum.feign;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.compilations.CompilationDto;

import java.util.List;

@FeignClient(name = "additional-functionality", path = "/compilations", configuration = FeignConfig.class)
public interface PublicCompilationClient {

    @GetMapping
    List<CompilationDto> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @Min(0) @RequestParam(defaultValue = "0") Integer from,
            @Positive @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/{compId}")
    CompilationDto getCompilationById(@Positive @PathVariable Long compId);
}
