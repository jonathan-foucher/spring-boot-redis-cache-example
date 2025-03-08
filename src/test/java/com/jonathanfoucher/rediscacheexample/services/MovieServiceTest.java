package com.jonathanfoucher.rediscacheexample.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jonathanfoucher.rediscacheexample.data.dto.MovieDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnableCaching
@SpringJUnitConfig({MovieService.class})
class MovieServiceTest {
    @Autowired
    private MovieService movieService;
    @MockitoBean
    private RedisTemplate<String, MovieDto> redisTemplate;
    @MockitoBean
    private FakeService fakeService;

    @MockitoBean
    private Cache allMoviesCache;
    @MockitoBean
    private Cache.ValueWrapper allMoviesValueWrapper;
    @MockitoBean
    private ValueOperations<String, MovieDto> opsForValue;

    @MockitoBean
    private CacheManager cacheManager;
    @MockitoBean
    private Cache movieCache;
    @MockitoBean
    private Cache.ValueWrapper movieValueWrapper;

    private static final Logger log = (Logger) LoggerFactory.getLogger(MovieService.class);
    private static final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    private static final Long ID = 15L;
    private static final String TITLE = "Some movie";
    private static final LocalDate RELEASE_DATE = LocalDate.of(2022, 7, 19);

    private static final String MOVIE_CACHE_NAME = "movies";
    private static final String ALL_MOVIES_CACHE_NAME = "all_movies";
    private static final String ALL_MOVIES_KEY = "findAllCached";

    @BeforeEach
    void init() {
        listAppender.list.clear();
        listAppender.start();
        log.addAppender(listAppender);
    }

    @AfterEach
    void reset() {
        log.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void findAllCachedInCache() {
        // GIVEN
        MovieDto movie = initMovie();

        when(cacheManager.getCache(ALL_MOVIES_CACHE_NAME))
                .thenReturn(allMoviesCache);
        when(allMoviesCache.get(ALL_MOVIES_KEY))
                .thenReturn(allMoviesValueWrapper);
        when(allMoviesValueWrapper.get())
                .thenReturn(List.of(movie));

        // WHEN
        List<MovieDto> results = movieService.findAllCached();

        // THEN
        verify(cacheManager, times(1)).getCache(ALL_MOVIES_CACHE_NAME);
        verify(allMoviesCache, times(1)).get(ALL_MOVIES_KEY);
        verify(allMoviesValueWrapper, times(1)).get();
        verify(redisTemplate, never()).keys(anyString());
        verify(redisTemplate, never()).opsForValue();
        verify(opsForValue, never()).get(anyString());
        verify(allMoviesCache, never()).put(anyString(), any(List.class));

        assertNotNull(results);
        assertEquals(1, results.size());

        MovieDto result = results.getFirst();
        checkMovie(result);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(0, logsList.size());
    }

    @Test
    void findAllCachedNotInCache() {
        // GIVEN
        MovieDto movie = initMovie();

        when(cacheManager.getCache(ALL_MOVIES_CACHE_NAME))
                .thenReturn(allMoviesCache);
        when(allMoviesCache.get(ALL_MOVIES_KEY))
                .thenReturn(null);
        when(redisTemplate.keys(MOVIE_CACHE_NAME + ":*"))
                .thenReturn(Set.of(String.valueOf(ID)));
        when(redisTemplate.opsForValue())
                .thenReturn(opsForValue);
        when(opsForValue.get(String.valueOf(ID)))
                .thenReturn(movie);

        // WHEN
        List<MovieDto> results = movieService.findAllCached();

        // THEN
        ArgumentCaptor<List<MovieDto>> capturedMovies = ArgumentCaptor.forClass(List.class);
        verify(cacheManager, times(1)).getCache(ALL_MOVIES_CACHE_NAME);
        verify(allMoviesCache, times(1)).get(ALL_MOVIES_KEY);
        verify(allMoviesValueWrapper, never()).get();
        verify(redisTemplate, times(1)).keys(MOVIE_CACHE_NAME + ":*");
        verify(redisTemplate, times(1)).opsForValue();
        verify(opsForValue, times(1)).get(String.valueOf(ID));
        verify(allMoviesCache, times(1)).put(eq(ALL_MOVIES_KEY), capturedMovies.capture());

        assertNotNull(results);
        assertEquals(1, results.size());

        MovieDto result = results.getFirst();
        checkMovie(result);

        List<MovieDto> cachedMovies = capturedMovies.getValue();
        assertNotNull(cachedMovies);
        assertEquals(1, cachedMovies.size());

        MovieDto cachedMovie = cachedMovies.getFirst();
        checkMovie(cachedMovie);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertNotNull(logsList.getFirst());
        assertEquals(Level.INFO, logsList.getFirst().getLevel());
        assertEquals("Get all cached movies", logsList.getFirst().getFormattedMessage());
    }

    @Test
    void findByIdInCache() {
        // GIVEN
        MovieDto movie = initMovie();

        when(cacheManager.getCache(MOVIE_CACHE_NAME))
                .thenReturn(movieCache);
        when(movieCache.get(ID))
                .thenReturn(movieValueWrapper);
        when(movieValueWrapper.get())
                .thenReturn(movie);

        // WHEN
        MovieDto result = movieService.findById(ID);

        // THEN
        verify(cacheManager, times(1)).getCache(MOVIE_CACHE_NAME);
        verify(movieCache, times(1)).get(ID);
        verify(movieValueWrapper, times(1)).get();
        verify(movieCache, never()).put(anyString(), any(MovieDto.class));
        verify(allMoviesCache, never()).clear();
        verify(fakeService, never()).findById(any());

        checkMovie(result);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(0, logsList.size());
    }

    @Test
    void findByIdNotInCache() {
        // GIVEN
        MovieDto movie = initMovie();

        when(cacheManager.getCache(MOVIE_CACHE_NAME))
                .thenReturn(movieCache);
        when(movieCache.get(ID))
                .thenReturn(null);

        when(cacheManager.getCache(ALL_MOVIES_CACHE_NAME))
                .thenReturn(allMoviesCache);

        when(fakeService.findById(ID))
                .thenReturn(movie);

        // WHEN
        MovieDto result = movieService.findById(ID);

        // THEN
        ArgumentCaptor<MovieDto> capturedMovie = ArgumentCaptor.forClass(MovieDto.class);
        verify(cacheManager, times(1)).getCache(MOVIE_CACHE_NAME);
        verify(movieCache, times(1)).get(ID);
        verify(movieValueWrapper, never()).get();
        verify(movieCache, times(1)).put(eq(ID), capturedMovie.capture());
        verify(allMoviesCache, times(1)).clear();
        verify(fakeService, times(1)).findById(ID);

        checkMovie(result);

        MovieDto cachedMovie = capturedMovie.getValue();
        checkMovie(cachedMovie);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(2, logsList.size());

        assertNotNull(logsList.get(0));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(String.format("Clear all entries for %s cache", ALL_MOVIES_CACHE_NAME), logsList.get(0).getFormattedMessage());

        assertNotNull(logsList.get(1));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(String.format("Get movie by id: %s", ID), logsList.get(1).getFormattedMessage());
    }

    @Test
    void findByIdNotInCacheWithNullResult() {
        // GIVEN
        when(cacheManager.getCache(MOVIE_CACHE_NAME))
                .thenReturn(movieCache);
        when(movieCache.get(ID))
                .thenReturn(null);

        when(cacheManager.getCache(ALL_MOVIES_CACHE_NAME))
                .thenReturn(allMoviesCache);

        when(fakeService.findById(ID))
                .thenReturn(null);

        // WHEN
        MovieDto result = movieService.findById(ID);

        // THEN
        verify(cacheManager, times(1)).getCache(MOVIE_CACHE_NAME);
        verify(movieCache, times(1)).get(ID);
        verify(movieValueWrapper, never()).get();
        verify(movieCache, never()).put(anyString(), any(MovieDto.class));
        verify(allMoviesCache, times(1)).clear();
        verify(fakeService, times(1)).findById(ID);

        assertNull(result);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(2, logsList.size());

        assertNotNull(logsList.get(0));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(String.format("Clear all entries for %s cache", ALL_MOVIES_CACHE_NAME), logsList.get(0).getFormattedMessage());

        assertNotNull(logsList.get(1));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(String.format("Get movie by id: %s", ID), logsList.get(1).getFormattedMessage());
    }

    @Test
    void addMovieToCache() {
        // GIVEN
        MovieDto movie = initMovie();

        when(cacheManager.getCache(MOVIE_CACHE_NAME))
                .thenReturn(movieCache);

        when(cacheManager.getCache(ALL_MOVIES_CACHE_NAME))
                .thenReturn(allMoviesCache);

        // WHEN
        MovieDto result = movieService.addMovieToCache(movie);

        // THEN
        ArgumentCaptor<MovieDto> capturedMovie = ArgumentCaptor.forClass(MovieDto.class);
        verify(cacheManager, times(1)).getCache(MOVIE_CACHE_NAME);
        verify(movieCache, times(1)).put(eq(ID), capturedMovie.capture());
        verify(allMoviesCache, times(1)).clear();

        checkMovie(result);

        MovieDto cachedMovie = capturedMovie.getValue();
        checkMovie(cachedMovie);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(2, logsList.size());

        assertNotNull(logsList.get(0));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(String.format("Clear all entries for %s cache", ALL_MOVIES_CACHE_NAME), logsList.get(0).getFormattedMessage());

        assertNotNull(logsList.get(1));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(String.format("Adding movie %s to %s cache", movie, MOVIE_CACHE_NAME), logsList.get(1).getFormattedMessage());
    }

    @Test
    void clearCache() {
        // GIVEN
        when(cacheManager.getCache(MOVIE_CACHE_NAME))
                .thenReturn(movieCache);

        when(cacheManager.getCache(ALL_MOVIES_CACHE_NAME))
                .thenReturn(allMoviesCache);

        // WHEN
        movieService.clearCache();

        // THEN
        verify(cacheManager, times(1)).getCache(MOVIE_CACHE_NAME);
        verify(movieCache, times(1)).clear();
        verify(allMoviesCache, times(1)).clear();

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(2, logsList.size());

        assertNotNull(logsList.get(0));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(String.format("Clear all entries for %s cache", ALL_MOVIES_CACHE_NAME), logsList.get(0).getFormattedMessage());

        assertNotNull(logsList.get(1));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(String.format("Clean all entries for %s cache", MOVIE_CACHE_NAME), logsList.get(1).getFormattedMessage());
    }

    @Test
    void clearCacheById() {
        // GIVEN
        when(cacheManager.getCache(MOVIE_CACHE_NAME))
                .thenReturn(movieCache);

        when(cacheManager.getCache(ALL_MOVIES_CACHE_NAME))
                .thenReturn(allMoviesCache);

        // WHEN
        movieService.cleanCacheById(ID);

        // THEN
        verify(cacheManager, times(1)).getCache(MOVIE_CACHE_NAME);
        verify(movieCache, times(1)).evict(ID);
        verify(allMoviesCache, times(1)).clear();

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(2, logsList.size());

        assertNotNull(logsList.get(0));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(String.format("Clear all entries for %s cache", ALL_MOVIES_CACHE_NAME), logsList.get(0).getFormattedMessage());

        assertNotNull(logsList.get(1));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(String.format("Clean entry %s for %s cache", ID, MOVIE_CACHE_NAME), logsList.get(1).getFormattedMessage());
    }

    private MovieDto initMovie() {
        MovieDto movie = new MovieDto();
        movie.setId(ID);
        movie.setTitle(TITLE);
        movie.setReleaseDate(RELEASE_DATE);
        return movie;
    }

    private void checkMovie(MovieDto movie) {
        assertNotNull(movie);
        assertEquals(ID, movie.getId());
        assertEquals(TITLE, movie.getTitle());
        assertEquals(RELEASE_DATE, movie.getReleaseDate());
    }
}
