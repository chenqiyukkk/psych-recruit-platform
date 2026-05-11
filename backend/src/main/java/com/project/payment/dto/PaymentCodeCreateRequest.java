package com.project.payment.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCodeCreateRequest {
  @NotBlank private String paymentType;
  @NotBlank private String qrCodeUrl;
  @NotNull private Boolean isDefault;
}

