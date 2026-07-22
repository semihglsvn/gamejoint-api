package com.gamejoint.gamejoint_api.dto;
import lombok.Data;

@Data
public class PasswordChangeRequest {
	private String otpCode;
    private String newPassword;
}