package com.gamejoint.gamejoint_api.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.Set;

@Data
public class GameDetail {
    private Long id;
    private String title;
    private String description;
    private String developer;
    private String publisher;
    private LocalDate releaseDate;
    private String esrbRating;
    private Integer metascore;
    private String coverImage;
    
    // You can even include sets of other DTOs here to send the genres and platforms!
    private Set<String> genreNames; 
    private Set<String> platformNames;
}