package com.gamejoint.gamejoint_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    
    private Long id;
    
    // Flattens the User relationship. The mobile app just needs the name to display.
    private String authorUsername; 
    
    private Integer score;
    
    private String comment;
    
    private LocalDateTime createdAt;
    
    // Keeping this structure as you requested! 
    // The mobile app could use this to show a grey "Pending Approval" tag on a user's own review.
    private String status; 
}