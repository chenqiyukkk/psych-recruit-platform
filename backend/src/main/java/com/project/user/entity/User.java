package com.project.user.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64, unique = true)
  private String username;

  @Column(nullable = false, length = 255)
  private String password;

  @Column(length = 32)
  private String phone;

  @Column(length = 128)
  private String email;

  @Column(nullable = false, length = 16)
  private String role;

  @Column(nullable = false)
  private Integer reputationScore = 100;

  @Column(precision = 3, scale = 2)
  private BigDecimal researcherRating;

  @Column(nullable = false)
  private Integer totalReviews = 0;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;
}

