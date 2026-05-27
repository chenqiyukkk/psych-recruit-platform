package com.project.reputation.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReputationResponse {

    private Long userId;

    private String username;

    private Integer reputationScore;


}
