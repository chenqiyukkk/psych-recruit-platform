package com.project.experiment.entity;

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
@Table(name = "experiments")
public class Experiment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(length = 255)
  private String location;

  @Column(nullable = false)
  private LocalDateTime startTime;

  @Column(nullable = false)
  private LocalDateTime endTime;

  @Column(length = 128)
  private String ethicsApprovalNo;

  @Column(nullable = false, length = 16)
  private String riskLevel;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal paymentAmount;

  @Column(nullable = false, length = 16)
  private String paymentMethod;

  @Column(length = 255)
  private String paymentDescription;

  @Column(columnDefinition = "json")
  private String screeningCriteria;

  @Column(columnDefinition = "json")
  private String excludeTags;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(nullable = false)
  private Long organizerId;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;
}

