package com.gamejoint.gamejoint_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PublicProfileResponse {
    private Long id;
    private String username;
    private LocalDateTime createdAt;
    private String roleName;
    private Boolean isBanned;
}