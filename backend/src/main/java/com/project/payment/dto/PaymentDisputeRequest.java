package com.project.payment.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentDisputeRequest {
  @NotNull private Long registrationId;
}

