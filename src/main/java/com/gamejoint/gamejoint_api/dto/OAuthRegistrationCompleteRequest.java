package com.gamejoint.gamejoint_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OAuthRegistrationCompleteRequest {

    @NotBlank(message = "Provider is required.")
    private String provider;

    @NotBlank(message = "Provider token is required.")
    private String providerToken;

    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores.")
    private String username;

    @NotNull(message = "Date of birth is required.")
    private LocalDate dob;

    private String cfTurnstileResponse;
}