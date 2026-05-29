package com.project.reputation.dto;


import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ReputationAdjustRequest {
    @NotNull
    private Integer scoreDelta;

    private String reason;
}
