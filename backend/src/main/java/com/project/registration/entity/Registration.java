package com.project.registration.entity;


import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_id",nullable = false)
    private Long experimentId;

    @Column(name = "user_id",nullable = false)
    private Long userId;

    @Column(nullable = false,length = 16)
    private String status;

    @Column(name = "applied_at",nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "sign_in_time")
    private LocalDateTime signInTime;

    @Column(name = "is_completed",nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",nullable = false)
    private LocalDateTime updatedAt;
}
