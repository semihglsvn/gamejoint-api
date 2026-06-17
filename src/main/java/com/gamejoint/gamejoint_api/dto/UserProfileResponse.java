package com.gamejoint.gamejoint_api.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserProfileResponse {
    
    private Long id;
    private String username;
    private String email;
    private LocalDate dob;
    private Boolean isVerified; 
    private LocalDateTime createdAt; 
    private String roleName; 
    private Boolean isBanned;
    private LocalDateTime banExpiresAt;

}