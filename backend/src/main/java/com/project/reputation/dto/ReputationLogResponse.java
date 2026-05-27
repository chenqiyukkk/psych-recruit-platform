package com.project.reputation.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReputationLogResponse {

    private Long id;

    private Long userId;

    private String changeType;

    private Integer scoreDelta;

    private String reason;

    private LocalDateTime createdAt;
}
