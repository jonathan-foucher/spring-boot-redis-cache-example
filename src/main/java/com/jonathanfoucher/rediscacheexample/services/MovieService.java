package com.jonathanfoucher.rediscacheexample.services;

import com.jonathanfoucher.rediscacheexample.data.dto.MovieDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {
    private final CacheManager cacheManager;
    private final RedisTemplate<String, MovieDto> redisTemplate;
    private final FakeService fakeService;

    private static final String MOVIE_CACHE_NAME = "movies";
    private static final String ALL_MOVIES_CACHE_NAME = "all_movies";

    @Cacheable(value = ALL_MOVIES_CACHE_NAME, unless = "#result == null or #result.isEmpty()", key = "#root.methodName")
    public List<MovieDto> findAllCached() {
        log.info("Get all cached movies");
        return redisTemplate.keys(MOVIE_CACHE_NAME + ":*")
                .stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .toList();
    }

    @Cacheable(value = MOVIE_CACHE_NAME, unless = "#result == null")
    public MovieDto findById(Long id) {
        clearFindAllMoviesCache();
        log.info("Get movie by id: {}", id);
        return fakeService.findById(id);
    }

    @CachePut(value = MOVIE_CACHE_NAME, key = "#result.id")
    public MovieDto addMovieToCache(MovieDto movie) {
        clearFindAllMoviesCache();
        log.info("Adding movie {} to {} cache", movie, MOVIE_CACHE_NAME);
        return movie;
    }

    @CacheEvict(value = MOVIE_CACHE_NAME, allEntries = true)
    public void clearCache() {
        clearFindAllMoviesCache();
        log.info("Clean all entries for {} cache", MOVIE_CACHE_NAME);
    }

    @CacheEvict(value = MOVIE_CACHE_NAME)
    public void cleanCacheById(Long id) {
        clearFindAllMoviesCache();
        log.info("Clean entry {} for {} cache", id, MOVIE_CACHE_NAME);
    }

    private void clearFindAllMoviesCache() {
        log.info("Clear all entries for {} cache", ALL_MOVIES_CACHE_NAME);
        Cache cache = cacheManager.getCache(ALL_MOVIES_CACHE_NAME);
        if (cache != null) {
            cache.clear();
        } else {
            log.error("Cache {} not found", ALL_MOVIES_CACHE_NAME);
        }
    }
}
