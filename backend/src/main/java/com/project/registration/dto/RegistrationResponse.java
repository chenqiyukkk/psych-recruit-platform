package com.project.registration.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RegistrationResponse {

    private Long id;

    private Long experimentId;

    private Long userId;

    private String status;

    private LocalDateTime appliedAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime signInTime;

    private Boolean isCompleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
