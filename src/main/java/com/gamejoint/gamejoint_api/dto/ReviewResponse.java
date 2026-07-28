package com.gamejoint.gamejoint_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    
    private Long id;
    private String authorUsername; 
        
    private String authorRole; 
    private Long gameId;
    private String gameTitle;
    
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
    private String status; 
}