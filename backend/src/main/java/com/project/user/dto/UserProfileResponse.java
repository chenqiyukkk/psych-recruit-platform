package com.project.user.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {
  private Long id;
  private String username;
  private String phone;
  private String email;
  private String role;
  private Integer reputationScore;
  private BigDecimal researcherRating;
  private Integer totalReviews;
}

