package com.gamejoint.gamejoint_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    
    private Long id;
    private String authorUsername; 
    
    // ADD THIS: So the mobile app knows if they get a Critic Badge
    private String authorRole; 
    
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
    private String status; 
}