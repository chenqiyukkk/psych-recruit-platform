package com.project.user.dto;

import com.project.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserListResponse {
  private Long id;
  private String username;
  private String phone;
  private String email;
  private String role;
  private Integer reputationScore;
  private BigDecimal researcherRating;
  private Integer totalReviews;
  private LocalDateTime createdAt;

  public static UserListResponse from(User user) {
    UserListResponse r = new UserListResponse();
    r.setId(user.getId());
    r.setUsername(user.getUsername());
    r.setPhone(user.getPhone());
    r.setEmail(user.getEmail());
    r.setRole(user.getRole());
    r.setReputationScore(user.getReputationScore());
    r.setResearcherRating(user.getResearcherRating());
    r.setTotalReviews(user.getTotalReviews());
    r.setCreatedAt(user.getCreatedAt());
    return r;
  }
}
