package com.gamejoint.gamejoint_api.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReportCreateRequest {
    
    // The exact review being reported
    private Long reviewId;
    
    // Accepts an array like ["Spam", "Harassment"] directly from the mobile app
    private List<String> reasons;
    
    // EXCLUDED FOR SECURITY:
    // - reporterId (We will securely extract this from their JWT token)
    // - status (Automatically defaults to PENDING in the Entity)
}