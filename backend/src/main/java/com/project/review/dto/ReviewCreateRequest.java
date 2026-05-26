package com.project.review.dto;


import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ReviewCreateRequest {


    @NotBlank
    private String reviewType;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Min(1)
    @Max(5)
    private Integer communicationScore;

    @Min(1)
    @Max(5)
    private Integer professionalismScore;

    @Min(1)
    @Max(5)
    private Integer punctualityScore;

    private String comment;

    private Boolean isAnonymous;
}
