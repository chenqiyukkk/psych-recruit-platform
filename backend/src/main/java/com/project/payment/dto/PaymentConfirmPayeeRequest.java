package com.project.payment.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentConfirmPayeeRequest {
  @NotNull private Long registrationId;
}

