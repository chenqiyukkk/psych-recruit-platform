package com.project.reputation.entity;


import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reputation_logs")
public class ReputationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "user_id",nullable = false)
    private Long userId;

    @Column(name = "registration_id")
    private Long registrationId;

    @Column(name = "change_type",nullable = false,length = 64)
    private String changeType;

    @Column(name = "score_delta",nullable = false)
    private Integer scoreDelta;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;
}
