package ru.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilations.CompilationDto;
import ru.practicum.dto.compilations.NewCompilationDto;
import ru.practicum.dto.compilations.UpdateCompilationRequest;

@FeignClient(name = "admin-compilations", path = "/admin/compilations", configuration = FeignConfig.class)
public interface AdminCompilationsClient {
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    CompilationDto createCompilation(@Valid @RequestBody NewCompilationDto dto);

    @PatchMapping("/{compId}")
    CompilationDto updateCompilation(@Positive @PathVariable Long compId, @Valid @RequestBody UpdateCompilationRequest request);

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCompilation(@Positive @PathVariable Long compId);

}
