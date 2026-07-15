package com.project.user.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
  private String phone;
  private String email;
  private String gender;
  private String ageGroup;
  private String majorCategory;
  private String handedness;
}

