package com.jonathanfoucher.rediscacheexample.services;

import com.jonathanfoucher.rediscacheexample.data.dto.MovieDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FakeService {
    // simulate retrieving data
    public MovieDto findById(Long id) {
        MovieDto movie = new MovieDto();
        movie.setId(id);
        movie.setTitle("Title");
        movie.setReleaseDate(LocalDate.of(2020, 1, 1));
        return movie;
    }
}
