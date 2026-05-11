package com.project.payment.entity;

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
@Table(name = "payment_records")
public class PaymentRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Long registrationId;

  @Column(nullable = false)
  private Long payerUserId;

  @Column(nullable = false)
  private Long payeeUserId;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(length = 1024)
  private String paymentScreenshotUrl;

  private LocalDateTime payerConfirmedAt;

  private LocalDateTime payeeConfirmedAt;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(nullable = false)
  private LocalDateTime createdAt;
}

