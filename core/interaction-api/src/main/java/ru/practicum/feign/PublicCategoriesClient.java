package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.categories.CategoryDto;

import java.util.List;

@FeignClient(name = "categories-service", path = "/categories", configuration = FeignConfig.class)

public interface PublicCategoriesClient {


    @GetMapping
    List<CategoryDto> getCategories(
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    );

    @GetMapping("/{catId}")
    CategoryDto getCategoryById(@PathVariable Long catId);
}
