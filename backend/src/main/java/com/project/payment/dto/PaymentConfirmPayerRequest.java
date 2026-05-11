package com.project.payment.dto;

import java.math.BigDecimal;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentConfirmPayerRequest {
  @NotNull private Long registrationId;
  @NotNull private Long payeeUserId;
  @NotNull private BigDecimal amount;
  private String paymentScreenshotUrl;
}

