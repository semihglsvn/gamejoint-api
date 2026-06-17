package com.gamejoint.gamejoint_api.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserRegistrationRequest {
    
    // Exactly matches your "Create an Account" form
    private String username;
    private String email;
    private LocalDate dob;
    private String password;
    
    // Required to verify the Cloudflare Turnstile captcha on the backend!
    private String cfTurnstileResponse; 
}