package com.gamejoint.gamejoint_api.dto;

import java.util.List;

import lombok.Data;

@Data
public class FeaturedGameResponse {
    private Long gameId;
    private String title;
    
    // The big, wide image for the top carousel
    private String customBanner; 
    private Integer metascore;          	
    private List<String> genres;         
    private List<String> platforms;
    private String coverImage;   
}