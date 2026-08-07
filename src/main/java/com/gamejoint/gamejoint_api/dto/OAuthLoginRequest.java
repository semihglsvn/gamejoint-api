package com.gamejoint.gamejoint_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthLoginRequest {

    @NotBlank(message = "Provider is required (e.g., GOOGLE, STEAM).")
    private String provider;

    @NotBlank(message = "Provider token is required.")
    private String providerToken; 

    private String cfTurnstileResponse; 
}