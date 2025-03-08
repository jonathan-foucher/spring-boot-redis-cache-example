package com.jonathanfoucher.rediscacheexample.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jonathanfoucher.rediscacheexample.data.dto.MovieDto;
import com.jonathanfoucher.rediscacheexample.services.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(MovieController.class)
class MovieControllerTest {
    private MockMvc mockMvc;
    @Autowired
    private MovieController movieController;
    @MockitoBean
    private MovieService movieService;

    private static final String MOVIES_PATH = "/movies";
    private static final String MOVIES_BY_ID_PATH = "/movies/{id}";
    private static final String MOVIES_CACHE_PATH = "/movies/cache";
    private static final String MOVIES_CACHE_BY_ID_PATH = "/movies/{id}/cache";

    private static final Long ID = 15L;
    private static final String TITLE = "Some movie";
    private static final LocalDate RELEASE_DATE = LocalDate.of(2022, 7, 19);

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = JsonMapper.builder().
                addModule(new JavaTimeModule())
                .build();
    }

    @BeforeEach
    void initEach() {
        mockMvc = MockMvcBuilders.standaloneSetup(movieController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void findAllCached() throws Exception {
        // GIVEN
        MovieDto movie = initMovie();

        when(movieService.findAllCached())
                .thenReturn(List.of(movie));

        // WHEN / THEN
        mockMvc.perform(get(MOVIES_PATH))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(List.of(movie))));

        verify(movieService, times(1)).findAllCached();
    }

    @Test
    void findAllCachedWithoutResult() throws Exception {
        // GIVEN
        when(movieService.findAllCached())
                .thenReturn(emptyList());

        // WHEN / THEN
        mockMvc.perform(get(MOVIES_PATH))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(emptyList())));

        verify(movieService, times(1)).findAllCached();
    }

    @Test
    void findById() throws Exception {
        // GIVEN
        MovieDto movie = initMovie();

        when(movieService.findById(ID))
                .thenReturn(movie);

        // WHEN / THEN
        mockMvc.perform(get(MOVIES_BY_ID_PATH, ID))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(movie)));

        verify(movieService, times(1)).findById(ID);
    }

    @Test
    void findByIdWithoutResult() throws Exception {
        // GIVEN
        when(movieService.findById(ID))
                .thenReturn(null);

        // WHEN / THEN
        mockMvc.perform(get(MOVIES_BY_ID_PATH, ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(movieService, times(1)).findById(ID);
    }

    @Test
    void addMovieToCache() throws Exception {
        // GIVEN
        MovieDto movie = initMovie();

        // WHEN / THEN
        mockMvc.perform(post(MOVIES_PATH).contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movie)))
                .andExpect(status().isOk());

        ArgumentCaptor<MovieDto> capturedMovie = ArgumentCaptor.forClass(MovieDto.class);
        verify(movieService, times(1)).addMovieToCache(capturedMovie.capture());

        MovieDto result = capturedMovie.getValue();
        assertNotNull(result);
        assertEquals(ID, result.getId());
        assertEquals(TITLE, result.getTitle());
        assertEquals(RELEASE_DATE, result.getReleaseDate());
    }

    @Test
    void clearCache() throws Exception {
        // WHEN / THEN
        mockMvc.perform(delete(MOVIES_CACHE_PATH))
                .andExpect(status().isOk());

        verify(movieService, times(1)).clearCache();
    }

    @Test
    void cleanCacheById() throws Exception {
        // WHEN / THEN
        mockMvc.perform(delete(MOVIES_CACHE_BY_ID_PATH, ID))
                .andExpect(status().isOk());

        verify(movieService, times(1)).cleanCacheById(ID);
    }

    private MovieDto initMovie() {
        MovieDto movie = new MovieDto();
        movie.setId(ID);
        movie.setTitle(TITLE);
        movie.setReleaseDate(RELEASE_DATE);
        return movie;
    }
}
