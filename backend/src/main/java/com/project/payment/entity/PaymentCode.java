package com.project.payment.entity;

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
@Table(name = "payment_codes")
public class PaymentCode {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false, length = 16)
  private String paymentType;

  @Column(nullable = false, length = 1024)
  private String qrCodeUrl;

  @Column(nullable = false)
  private Boolean isDefault;

  @Column(nullable = false)
  private LocalDateTime createdAt;
}

