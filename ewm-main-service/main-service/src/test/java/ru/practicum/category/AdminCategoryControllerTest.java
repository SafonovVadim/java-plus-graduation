package ru.practicum.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.categories.AdminCategoryController;
import ru.practicum.categories.service.CategoryService;
import ru.practicum.categories.CategoryDto;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCategoryController.class)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createCategory_shouldReturnCreatedCategory() throws Exception {
        CategoryDto request = new CategoryDto();
        request.setName("Conferences");

        CategoryDto response = new CategoryDto();
        response.setId(1L);
        response.setName("Conferences");

        when(categoryService.createCategory(any(CategoryDto.class))).thenReturn(response);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Conferences"));
    }

    @Test
    void updateCategory_shouldReturnUpdatedCategory() throws Exception {
        CategoryDto request = new CategoryDto();
        request.setName("Tech Conferences");

        CategoryDto response = new CategoryDto();
        response.setId(1L);
        response.setName("Tech Conferences");

        when(categoryService.updateCategory(eq(1L), any(CategoryDto.class))).thenReturn(response);

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tech Conferences"));
    }

    @Test
    void createCategory_duplicateName_shouldReturn409() throws Exception {
        CategoryDto request = new CategoryDto();
        request.setName("Music");

        when(categoryService.createCategory(request))
                .thenThrow(new ru.practicum.error.exception.ConflictException(
                        "Category with name 'Music' already exists"));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason").value("Conflict occurred."))
                .andExpect(jsonPath("$.message").value("Category with name 'Music' already exists"));
    }

    @Test
    void createCategory_invalidName_shouldReturn400() throws Exception {
        CategoryDto invalid = new CategoryDto();
        invalid.setName("");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_notFound_shouldReturn404() throws Exception {
        when(categoryService.updateCategory(eq(999L), any()))
                .thenThrow(new ru.practicum.error.exception.NotFoundException("Not found"));

        CategoryDto update = new CategoryDto();
        update.setName("New Name");

        mockMvc.perform(patch("/admin/categories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_inUse_shouldReturn409() throws Exception {
        doThrow(new ru.practicum.error.exception.ConflictException("Used"))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isConflict());
    }
}
