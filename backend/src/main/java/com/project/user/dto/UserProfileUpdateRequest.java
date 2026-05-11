package com.project.user.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
  private String phone;
  private String email;
}

