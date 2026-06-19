package com.gamejoint.gamejoint_api.dto;

import lombok.Data;

@Data
public class BanRequest {
    // If this is null, it's a permanent ban!
    private Integer durationDays; 
    
    // Optional, but highly recommended so moderators can leave a note
    private String reason; 
}