package com.project.experiment.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExperimentTagRequest {
  @NotBlank private String tagName;
  @NotNull private Integer coolingDays;
}

