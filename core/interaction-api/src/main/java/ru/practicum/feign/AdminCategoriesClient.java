package ru.practicum.feign;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.categories.CategoryDto;

@FeignClient(name = "category-service",contextId = "adminCategory", path = "/admin/categories", configuration = FeignConfig.class)
public interface AdminCategoriesClient {

    @PostMapping()
    ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto dto);

    @PatchMapping("/{catId}")
    ResponseEntity<CategoryDto> updateCategory(@PathVariable Long catId, @Valid @RequestBody CategoryDto dto);

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCategory(@PathVariable Long catId);

}
