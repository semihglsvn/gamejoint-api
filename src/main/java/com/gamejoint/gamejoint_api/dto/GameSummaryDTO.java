package com.gamejoint.gamejoint_api.dto;

import lombok.Data;

@Data
public class GameSummaryDTO {
    private Long id; 
    private String title;
    private String coverImage;
    private Integer metascore;
}