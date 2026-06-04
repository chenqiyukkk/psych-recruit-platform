package com.project.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WxLoginResponse {
  private String token;
  private UserProfileResponse profile;
}
