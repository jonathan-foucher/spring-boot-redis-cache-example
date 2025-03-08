package com.jonathanfoucher.rediscacheexample.controllers;

import com.jonathanfoucher.rediscacheexample.data.dto.MovieDto;
import com.jonathanfoucher.rediscacheexample.services.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/movies")
public class MovieController {
    private final MovieService movieService;

    @GetMapping
    public List<MovieDto> findAllCached() {
        return movieService.findAllCached();
    }

    @GetMapping("/{id}")
    public MovieDto findById(@PathVariable Long id) {
        return movieService.findById(id);
    }

    @PostMapping
    public void addMovieToCache(@RequestBody MovieDto movie) {
        movieService.addMovieToCache(movie);
    }

    @DeleteMapping("/cache")
    public void clearCache() {
        movieService.clearCache();
    }

    @DeleteMapping("/{id}/cache")
    public void cleanCacheById(@PathVariable Long id) {
        movieService.cleanCacheById(id);
    }
}
