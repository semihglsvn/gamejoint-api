package com.gamejoint.gamejoint_api.dto;

import lombok.Data;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class UserRegistrationRequest {
    
    // Exactly matches your "Create an Account" form
	@NotBlank(message = "Username is required.")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores.")
    private String username;
    private String email;
    private LocalDate dob;
    private String password;
    
    // Required to verify the Cloudflare Turnstile captcha on the backend!
    private String cfTurnstileResponse; 
}