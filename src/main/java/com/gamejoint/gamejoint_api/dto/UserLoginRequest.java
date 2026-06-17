package com.gamejoint.gamejoint_api.dto;

import lombok.Data;

@Data
public class UserLoginRequest {
    
    // Your UI allows logging in with EITHER username or email
    private String usernameOrEmail; 
    
    private String password;
    
    // Matches the checkbox on your "Welcome Back" screen
    private Boolean rememberMe; 
    
    // Cloudflare verification for the login attempt
    private String cfTurnstileResponse; 
}