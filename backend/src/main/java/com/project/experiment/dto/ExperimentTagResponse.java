package com.project.experiment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExperimentTagResponse {
  private Long id;
  private String tagName;
  private Integer coolingDays;
}

