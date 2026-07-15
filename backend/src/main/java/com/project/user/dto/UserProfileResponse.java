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
  private String gender;
  private String ageGroup;
  private String majorCategory;
  private String handedness;
  private Integer reputationScore;
  private BigDecimal researcherRating;
  private Integer totalReviews;

  public UserProfileResponse(Long id, String username, String phone, String email,
      String role, Integer reputationScore, BigDecimal researcherRating, Integer totalReviews) {
    this.id = id;
    this.username = username;
    this.phone = phone;
    this.email = email;
    this.role = role;
    this.reputationScore = reputationScore;
    this.researcherRating = researcherRating;
    this.totalReviews = totalReviews;
  }
}

