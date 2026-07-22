package com.gamejoint.gamejoint_api.dto;
import lombok.Data;

@Data
public class EmailChangeRequest {
private String otpCode;
private String newEmail;
}
