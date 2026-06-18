package com.gamejoint.gamejoint_api.dto;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UserProfileUpdateRequest {
    private String username;
    private LocalDate dob;
}