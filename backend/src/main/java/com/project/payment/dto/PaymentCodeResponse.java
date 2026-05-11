package com.project.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentCodeResponse {
  private Long id;
  private String paymentType;
  private String qrCodeUrl;
  private Boolean isDefault;
}

