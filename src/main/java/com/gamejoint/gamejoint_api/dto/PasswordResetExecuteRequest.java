package com.gamejoint.gamejoint_api.dto;
import lombok.Data;

@Data
public class PasswordResetExecuteRequest {
    private String token;
    private String newPassword;
}