package ru.practicum.compilation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.compilation.controller.PublicCompilationController;
import ru.practicum.compilation.service.CompilationService;

import ru.practicum.error.exception.NotFoundException;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicCompilationController.class)
class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    @Test
    void getCompilations_shouldReturnListOfCompilations() throws Exception {
        CompilationDto comp1 = CompilationDto.builder().id(1L).title("Comp 1").pinned(true).events(Collections.emptyList()).build();
        CompilationDto comp2 = CompilationDto.builder().id(2L).title("Comp 2").pinned(true).events(Collections.emptyList()).build();

        when(compilationService.getCompilations(true, 0, 10)).thenReturn(List.of(comp1, comp2));

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Comp 1"))
                .andExpect(jsonPath("$[1].title").value("Comp 2"));

        verify(compilationService).getCompilations(true, 0, 10);
    }

    @Test
    void getCompilationById_shouldReturnCompilation() throws Exception {
        CompilationDto response = CompilationDto.builder()
                .id(1L)
                .title("Winter Events")
                .pinned(false)
                .events(Collections.emptyList())
                .build();

        when(compilationService.getCompilationById(1L)).thenReturn(response);

        mockMvc.perform(get("/compilations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Winter Events"));

        verify(compilationService).getCompilationById(1L);
    }

    @Test
    void getCompilationById_notFound_shouldReturn404() throws Exception {
        when(compilationService.getCompilationById(999L))
                .thenThrow(new NotFoundException("Compilation with id=999 was not found"));

        mockMvc.perform(get("/compilations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."));
    }
}
