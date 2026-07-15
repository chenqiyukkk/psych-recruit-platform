package com.project.experiment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExperimentResponse {
  private Long id;
  private String title;
  private String description;
  private String location;
  private Integer participantLimit;
  private Long approvedCount;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String ethicsApprovalNo;
  private String riskLevel;
  private BigDecimal paymentAmount;
  private String paymentMethod;
  private String paymentDescription;
  private String screeningCriteria;
  private String excludeTags;
  private String status;
  private String reviewComment;
  private Long organizerId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<ExperimentTagResponse> tags;
}
