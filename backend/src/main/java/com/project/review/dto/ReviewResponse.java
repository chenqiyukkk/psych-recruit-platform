package com.project.review.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReviewResponse {

    private Long id;

    private Long registrationId;

    private Long reviewerId;

    private Long reviewedId;

    private String reviewType;

    private Integer rating;

    private  Integer communicationScore;

    private Integer professionalismScore;

    private Integer punctualityScore;

    private String comment;

    private Boolean isAnonymous;

    private LocalDateTime createdAt;


}
