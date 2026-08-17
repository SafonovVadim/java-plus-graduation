package ru.practicum.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.categories.CategoryController;
import ru.practicum.categories.service.CategoryService;
import ru.practicum.categories.CategoryDto;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void getCategories_shouldReturnListOfCategories() throws Exception {
        CategoryDto cat1 = new CategoryDto();
        cat1.setId(1L);
        cat1.setName("Music");

        CategoryDto cat2 = new CategoryDto();
        cat2.setId(2L);
        cat2.setName("Sport");

        when(categoryService.getCategories(0, 10)).thenReturn(List.of(cat1, cat2));

        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Music"))
                .andExpect(jsonPath("$[1].name").value("Sport"));

        verify(categoryService).getCategories(0, 10);
    }

    @Test
    void getCategoryById_shouldReturnCategory() throws Exception {
        CategoryDto dto = new CategoryDto();
        dto.setId(1L);
        dto.setName("Music");

        when(categoryService.getCategoryById(1L)).thenReturn(dto);

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Music"));
    }

    @Test
    void getCategoryById_notFound_shouldReturn404() throws Exception {
        when(categoryService.getCategoryById(999L))
                .thenThrow(new ru.practicum.error.exception.NotFoundException("Not found"));

        mockMvc.perform(get("/categories/999"))
                .andExpect(status().isNotFound());
    }
}
