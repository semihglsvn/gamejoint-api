package com.gamejoint.gamejoint_api.dto;
import lombok.Data;

@Data
public class AccountDeleteRequest {
    private String currentPassword;
}