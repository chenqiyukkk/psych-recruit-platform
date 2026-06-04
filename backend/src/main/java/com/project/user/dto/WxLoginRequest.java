package com.project.user.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WxLoginRequest {
  @NotBlank
  private String code;
}
