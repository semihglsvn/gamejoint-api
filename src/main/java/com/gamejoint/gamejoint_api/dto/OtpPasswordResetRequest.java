package com.gamejoint.gamejoint_api.dto;

import lombok.Data;

@Data
public class OtpPasswordResetRequest {
    
    private String email;
    
    // The 6-digit code
    private String otp;
    
    private String newPassword;
}