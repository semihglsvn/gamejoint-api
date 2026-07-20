package com.gamejoint.gamejoint_api.dto;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    
    // Can be their username or email
    private String identifier;
    
    // The 6-digit code
    private String otp;
}