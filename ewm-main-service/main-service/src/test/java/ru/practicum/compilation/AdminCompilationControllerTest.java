package ru.practicum.compilation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.compilation.controller.AdminCompilationController;
import ru.practicum.compilation.service.CompilationService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCompilationController.class)
class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    @Test
    void createCompilation_shouldReturnCreatedCompilationAndStatus201() throws Exception {
        NewCompilationDto request = new NewCompilationDto(List.of(1L, 2L), true, "Summer Events");

        CompilationDto response = CompilationDto.builder()
                .id(1L)
                .title("Summer Events")
                .pinned(true)
                .events(Collections.emptyList())
                .build();

        when(compilationService.createCompilation(any(NewCompilationDto.class))).thenReturn(response);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Summer Events"))
                .andExpect(jsonPath("$.pinned").value(true));

        verify(compilationService).createCompilation(any(NewCompilationDto.class));
    }

    @Test
    void createCompilation_withBlankTitle_shouldReturnBadRequest() throws Exception {
        NewCompilationDto request = new NewCompilationDto(List.of(), false, "");

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).createCompilation(any());
    }

    @Test
    void updateCompilation_shouldReturnUpdatedCompilation() throws Exception {
        UpdateCompilationRequest request = new UpdateCompilationRequest(null, false, "Updated Title");

        CompilationDto response = CompilationDto.builder()
                .id(1L)
                .title("Updated Title")
                .pinned(false)
                .events(Collections.emptyList())
                .build();

        when(compilationService.updateCompilation(eq(1L), any(UpdateCompilationRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/admin/compilations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.pinned").value(false));

        verify(compilationService).updateCompilation(eq(1L), any(UpdateCompilationRequest.class));
    }

    @Test
    void deleteCompilation_shouldReturnStatus204() throws Exception {
        doNothing().when(compilationService).deleteCompilation(1L);

        mockMvc.perform(delete("/admin/compilations/1"))
                .andExpect(status().isNoContent());

        verify(compilationService).deleteCompilation(1L);
    }
}
