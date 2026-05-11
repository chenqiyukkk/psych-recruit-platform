package com.project.experiment.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ExperimentQueryRequest {
  private String keyword;
  private String status;
  private String riskLevel;
  private String paymentMethod;
  private Long organizerId;
  private LocalDateTime startFrom;
  private LocalDateTime endTo;
}

