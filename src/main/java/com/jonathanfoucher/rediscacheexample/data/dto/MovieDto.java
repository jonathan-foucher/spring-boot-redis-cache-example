package com.jonathanfoucher.rediscacheexample.data.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MovieDto implements Serializable {
    private Long id;
    private String title;
    private LocalDate releaseDate;

    @Override
    public String toString() {
        return String.format("{ id=%s, title=\"%s\", release_date=%s }", id, title, releaseDate);
    }
}
