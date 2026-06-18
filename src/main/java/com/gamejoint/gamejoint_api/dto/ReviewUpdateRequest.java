package com.gamejoint.gamejoint_api.dto;

import lombok.Data;

@Data
public class ReviewUpdateRequest {
    private Integer score;
    private String comment;
}