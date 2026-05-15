package com.gamejoint.gamejoint_api.coremodels;

import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "games")
@Data
@EqualsAndHashCode(callSuper = true)
public class Game extends BaseEntity {

    private String title;
    
    @Column(columnDefinition = "text")
    private String description;
    
    private String developer;
    
    private String publisher;
    
    @Column(name = "release_date")
    private LocalDate releaseDate; 
    @Column(name = "esrb_rating")
    private String esrbRating;
    
    private Integer metascore;     
    @Column(name = "cover_image")
    private String coverImage;
    
    Set<Genre> genres;
    Set<Platform> platforms;

}