package com.gamejoint.gamejoint_api.dto;
import lombok.Data;

@Data
public class PasswordChangeRequest {
    private String currentPassword;
    private String newPassword;
}