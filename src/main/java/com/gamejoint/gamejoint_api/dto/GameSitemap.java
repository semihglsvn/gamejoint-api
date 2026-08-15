package com.gamejoint.gamejoint_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameSitemap {
    private Long id;
    private LocalDate updatedAt;
    private String title; // NEW: Required for SEO slug generation
}