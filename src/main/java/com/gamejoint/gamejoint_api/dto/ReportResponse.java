package com.gamejoint.gamejoint_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportResponse {
    
    private Long id;
    
    // Flattens the reporter relationship so the admin can see who submitted it
    private String reporterUsername;
    
    // The ID of the review so the admin panel can fetch and display the bad comment
    private Long reviewId;
    
    // The raw, comma-separated string straight from the database
    private String reasons;
    
    // e.g., PENDING, REVIEWED, or IGNORED
    private String status;
    
    private LocalDateTime createdAt;
}