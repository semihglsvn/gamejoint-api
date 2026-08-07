package com.gamejoint.gamejoint_api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OAuthAuthResponse {
    
    // If true, the frontend must show the Username/DOB form.
    // If false, the login was successful and the jwtToken is populated.
    private boolean isNewUser; 
    
    // Provided so the frontend can pre-fill "Signing in as: user@gmail.com" on the UI
    private String email; 
    
    // The standard GameJoint access token (Null if isNewUser = true)
    private String jwtToken; 
}