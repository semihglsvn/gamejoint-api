package com.gamejoint.gamejoint_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

@Entity
@Table(name = "platforms")
@Data
public class Platform  {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;
    
    private String name;

    // 1. mappedBy links this back to the Game class
    @ManyToMany(mappedBy = "platforms")
    
    // 2. SAFETY: Prevents infinite JSON data loops when sending to the mobile app
    @JsonIgnore 
    
    // 3. SAFETY: Prevents Lombok from crashing the server with StackOverflow errors
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Game> games;

}