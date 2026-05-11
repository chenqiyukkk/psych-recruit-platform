package com.project.user.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRegisterRequest {
  @NotBlank private String username;
  @NotBlank private String password;
  private String phone;
  private String email;
  @NotBlank private String role;
}

