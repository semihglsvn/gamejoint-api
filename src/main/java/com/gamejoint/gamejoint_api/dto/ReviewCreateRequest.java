package com.gamejoint.gamejoint_api.dto;

import lombok.Data;

@Data
public class ReviewCreateRequest {
    
    // We need to know which game they are reviewing
    private Long gameId;
    
    // Their rating
    private Integer score;
    
    // The actual text of the review
    private String comment;
    
    // Notice what is securely excluded:
    // - userId (Handled by Spring Security tokens)
    // - status (Handled by the database default 'APPROVED')
    // - modCleared (Handled by the backend)
}