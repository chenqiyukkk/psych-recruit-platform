package com.project.experiment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExperimentCreateRequest {
  @NotBlank private String title;
  private String description;
  private String location;

  @NotNull private LocalDateTime startTime;
  @NotNull private LocalDateTime endTime;

  private String ethicsApprovalNo;
  @NotBlank private String riskLevel;

  @NotNull private BigDecimal paymentAmount;
  @NotBlank private String paymentMethod;
  private String paymentDescription;

  private String screeningCriteria;
  private String excludeTags;

  private List<ExperimentTagRequest> tags;
}

