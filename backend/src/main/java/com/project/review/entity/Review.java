package com.project.review.entity;


import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reviews")
public class Review {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_id",nullable = false)
    private Long registrationId;

    @Column(name = "reviewer_id",nullable = false)
    private Long reviewerId;

    @Column(name = "reviewed_id",nullable = false)
    private Long reviewedId;

    @Column(name = "review_type",nullable = false)
    private String reviewType;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "communication_score")
    private Integer communicationScore;

    @Column(name = "professionalism_score")
    private Integer professionalismScore;

    @Column(name = "punctuality_score")
    private Integer punctualityScore;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_anonymous",nullable = false)
    private Boolean isAnonymous = false;

    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;
}
