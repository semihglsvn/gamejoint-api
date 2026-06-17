package com.gamejoint.gamejoint_api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "games")
@Data
@EntityListeners(AuditingEntityListener.class) // Tells Spring to auto-fill the createdAt date
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

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
    
    @ManyToMany
    @JoinTable(
        name = "game_genres", 
        joinColumns = @JoinColumn(name = "game_id"), 
        inverseJoinColumns = @JoinColumn(name = "genre_id") 
    )
    @EqualsAndHashCode.Exclude
    private Set<Genre> genres; // Added 'private' for standard encapsulation

    @ManyToMany
    @JoinTable(
        name = "game_platforms", 
        joinColumns = @JoinColumn(name = "game_id"), 
        inverseJoinColumns = @JoinColumn(name = "platform_id")
    )
    @EqualsAndHashCode.Exclude
    private Set<Platform> platforms; // Properly mapped the platforms table!

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}