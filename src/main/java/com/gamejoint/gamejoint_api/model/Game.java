package com.gamejoint.gamejoint_api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.hibernate.annotations.BatchSize; // --- NEW IMPORT ---
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "games")
@Data
@EntityListeners(AuditingEntityListener.class)
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
    @BatchSize(size = 50) // --- STOPS THE N+1 QUERY SPAM ---
    private Set<Genre> genres;

    @ManyToMany
    @JoinTable(
        name = "game_platforms", 
        joinColumns = @JoinColumn(name = "game_id"), 
        inverseJoinColumns = @JoinColumn(name = "platform_id")
    )
    @EqualsAndHashCode.Exclude
    @BatchSize(size = 50) // --- STOPS THE N+1 QUERY SPAM ---
    private Set<Platform> platforms; 

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
 // --- ADD THIS FIELD TO Game.java ---
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @BatchSize(size = 50)
    private Set<GameScreenshot> screenshots;
}