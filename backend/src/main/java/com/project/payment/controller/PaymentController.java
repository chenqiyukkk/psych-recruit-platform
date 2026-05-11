package com.project.payment.controller;

import com.project.common.api.Result;
import com.project.payment.dto.PaymentCodeCreateRequest;
import com.project.payment.dto.PaymentCodeResponse;
import com.project.payment.dto.PaymentConfirmPayerRequest;
import com.project.payment.dto.PaymentConfirmPayeeRequest;
import com.project.payment.dto.PaymentDisputeRequest;
import com.project.payment.dto.PaymentRecordResponse;
import com.project.payment.service.PaymentService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/codes")
  public Result<PaymentCodeResponse> uploadCode(
      Authentication authentication, @Valid @RequestBody PaymentCodeCreateRequest request) {
    return Result.success(paymentService.createPaymentCode(authentication.getName(), request));
  }

  @GetMapping("/codes/my")
  public Result<List<PaymentCodeResponse>> myCodes(Authentication authentication) {
    return Result.success(paymentService.myPaymentCodes(authentication.getName()));
  }

  @GetMapping("/records/{registrationId}")
  public Result<PaymentRecordResponse> record(
      Authentication authentication, @PathVariable("registrationId") Long registrationId) {
    return Result.success(paymentService.getPaymentRecord(registrationId, authentication.getName()));
  }

  @PostMapping("/records/confirm-payer")
  @PreAuthorize("hasAnyRole('研究者','管理员')")
  public Result<PaymentRecordResponse> confirmPayer(
      Authentication authentication, @Valid @RequestBody PaymentConfirmPayerRequest request) {
    return Result.success(paymentService.confirmPayer(authentication.getName(), request));
  }

  @PostMapping("/records/confirm-payee")
  public Result<PaymentRecordResponse> confirmPayee(
      Authentication authentication, @Valid @RequestBody PaymentConfirmPayeeRequest request) {
    return Result.success(paymentService.confirmPayee(authentication.getName(), request));
  }

  @PostMapping("/records/dispute")
  public Result<PaymentRecordResponse> dispute(
      Authentication authentication, @Valid @RequestBody PaymentDisputeRequest request) {
    return Result.success(paymentService.dispute(authentication.getName(), request));
  }
}

