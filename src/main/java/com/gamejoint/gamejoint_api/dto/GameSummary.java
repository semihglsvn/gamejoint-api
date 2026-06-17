package com.gamejoint.gamejoint_api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class GameSummary {
    private Long id;
    private String title;
    private String coverImage;
    
    // UI Requirement: The little grey score box
    // If this is 'null', your mobile/web app will know to display "tbd"
    private Integer metascore; 
    
    // UI Requirement: For client-side sorting or displaying "Released on..."
    private LocalDate releaseDate; 
    
    // UI Requirement: The small grey tags (PC, NINTENDO SWITCH, PLAYSTATION)
    // We use a Set of Strings here so the JSON is extremely lightweight
    private Set<String> platforms; 
    
    // Included so the mobile app can color-code or display genre tags if needed on other sliders
    private Set<String> genres; 
}