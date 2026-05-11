package com.project.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentRecordResponse {
  private Long id;
  private Long registrationId;
  private Long payerUserId;
  private Long payeeUserId;
  private BigDecimal amount;
  private String paymentScreenshotUrl;
  private LocalDateTime payerConfirmedAt;
  private LocalDateTime payeeConfirmedAt;
  private String status;
  private LocalDateTime createdAt;
}

