package com.project.payment.service;

import com.project.common.exception.ApiException;
import com.project.payment.PaymentConstants;
import com.project.payment.dto.PaymentCodeCreateRequest;
import com.project.payment.dto.PaymentCodeResponse;
import com.project.payment.dto.PaymentConfirmPayerRequest;
import com.project.payment.dto.PaymentConfirmPayeeRequest;
import com.project.payment.dto.PaymentDisputeRequest;
import com.project.payment.dto.PaymentRecordResponse;
import com.project.payment.entity.PaymentCode;
import com.project.payment.entity.PaymentRecord;
import com.project.payment.repo.PaymentCodeRepository;
import com.project.payment.repo.PaymentRecordRepository;
import com.project.user.UserRoles;
import com.project.user.repo.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentCodeRepository paymentCodeRepository;
  private final PaymentRecordRepository paymentRecordRepository;
  private final UserRepository userRepository;

  @Transactional
  public PaymentCodeResponse createPaymentCode(String username, PaymentCodeCreateRequest request) {
    if (!PaymentConstants.CODE_TYPES.contains(request.getPaymentType())) {
      throw new ApiException(400, "paymentType 不合法");
    }
    Long userId =
        userRepository
            .findByUsername(username)
            .map(u -> u.getId())
            .orElseThrow(() -> new ApiException(401, "未登录"));

    if (Boolean.TRUE.equals(request.getIsDefault())) {
      clearDefault(userId);
    }

    PaymentCode code = new PaymentCode();
    code.setUserId(userId);
    code.setPaymentType(request.getPaymentType());
    code.setQrCodeUrl(request.getQrCodeUrl());
    code.setIsDefault(request.getIsDefault());
    code.setCreatedAt(LocalDateTime.now());
    PaymentCode saved = paymentCodeRepository.save(code);
    return new PaymentCodeResponse(saved.getId(), saved.getPaymentType(), saved.getQrCodeUrl(), saved.getIsDefault());
  }

  public List<PaymentCodeResponse> myPaymentCodes(String username) {
    Long userId =
        userRepository
            .findByUsername(username)
            .map(u -> u.getId())
            .orElseThrow(() -> new ApiException(401, "未登录"));
    return paymentCodeRepository.findByUserIdOrderByIdDesc(userId).stream()
        .map(c -> new PaymentCodeResponse(c.getId(), c.getPaymentType(), c.getQrCodeUrl(), c.getIsDefault()))
        .collect(Collectors.toList());
  }

  public PaymentRecordResponse getPaymentRecord(Long registrationId, String username) {
    PaymentRecord record =
        paymentRecordRepository
            .findByRegistrationId(registrationId)
            .orElseThrow(() -> new ApiException(404, "支付记录不存在"));
    assertCanAccessRecord(record, username);
    return toResponse(record);
  }

  @Transactional
  public PaymentRecordResponse confirmPayer(String username, PaymentConfirmPayerRequest request) {
    var payer =
        userRepository.findByUsername(username).orElseThrow(() -> new ApiException(401, "未登录"));
    if (!Objects.equals(payer.getRole(), UserRoles.RESEARCHER)
        && !Objects.equals(payer.getRole(), UserRoles.ADMIN)) {
      throw new ApiException(403, "仅研究者可确认支付");
    }

    PaymentRecord record =
        paymentRecordRepository
            .findByRegistrationId(request.getRegistrationId())
            .orElseGet(
                () -> {
                  PaymentRecord r = new PaymentRecord();
                  r.setRegistrationId(request.getRegistrationId());
                  r.setCreatedAt(LocalDateTime.now());
                  r.setStatus(PaymentConstants.STATUS_PENDING);
                  return r;
                });

    if (!PaymentConstants.STATUS_PENDING.equals(record.getStatus())
        && !PaymentConstants.STATUS_PAID.equals(record.getStatus())) {
      throw new ApiException(400, "当前状态不可确认付款");
    }

    record.setPayerUserId(payer.getId());
    record.setPayeeUserId(request.getPayeeUserId());
    record.setAmount(request.getAmount());
    record.setPaymentScreenshotUrl(request.getPaymentScreenshotUrl());
    record.setPayerConfirmedAt(LocalDateTime.now());
    record.setStatus(PaymentConstants.STATUS_PAID);
    PaymentRecord saved = paymentRecordRepository.save(record);
    return toResponse(saved);
  }

  @Transactional
  public PaymentRecordResponse confirmPayee(String username, PaymentConfirmPayeeRequest request) {
    var payee =
        userRepository.findByUsername(username).orElseThrow(() -> new ApiException(401, "未登录"));
    PaymentRecord record =
        paymentRecordRepository
            .findByRegistrationId(request.getRegistrationId())
            .orElseThrow(() -> new ApiException(404, "支付记录不存在"));

    if (!Objects.equals(record.getPayeeUserId(), payee.getId())
        && !Objects.equals(payee.getRole(), UserRoles.ADMIN)) {
      throw new ApiException(403, "无权限");
    }
    if (!PaymentConstants.STATUS_PAID.equals(record.getStatus())) {
      throw new ApiException(400, "当前状态不可确认收款");
    }

    record.setPayeeConfirmedAt(LocalDateTime.now());
    record.setStatus(PaymentConstants.STATUS_CONFIRMED);
    return toResponse(paymentRecordRepository.save(record));
  }

  @Transactional
  public PaymentRecordResponse dispute(String username, PaymentDisputeRequest request) {
    var user = userRepository.findByUsername(username).orElseThrow(() -> new ApiException(401, "未登录"));
    PaymentRecord record =
        paymentRecordRepository
            .findByRegistrationId(request.getRegistrationId())
            .orElseThrow(() -> new ApiException(404, "支付记录不存在"));
    if (!Objects.equals(user.getRole(), UserRoles.ADMIN)
        && !Objects.equals(record.getPayerUserId(), user.getId())
        && !Objects.equals(record.getPayeeUserId(), user.getId())) {
      throw new ApiException(403, "无权限");
    }
    if (PaymentConstants.STATUS_CONFIRMED.equals(record.getStatus())) {
      throw new ApiException(400, "已确认的支付不可申诉");
    }
    record.setStatus(PaymentConstants.STATUS_DISPUTED);
    return toResponse(paymentRecordRepository.save(record));
  }

  private void clearDefault(Long userId) {
    List<PaymentCode> codes = paymentCodeRepository.findByUserIdOrderByIdDesc(userId);
    for (PaymentCode code : codes) {
      if (Boolean.TRUE.equals(code.getIsDefault())) {
        code.setIsDefault(false);
        paymentCodeRepository.save(code);
      }
    }
  }

  private void assertCanAccessRecord(PaymentRecord record, String username) {
    var user = userRepository.findByUsername(username).orElseThrow(() -> new ApiException(401, "未登录"));
    if (Objects.equals(user.getRole(), UserRoles.ADMIN)) {
      return;
    }
    if (Objects.equals(record.getPayerUserId(), user.getId())
        || Objects.equals(record.getPayeeUserId(), user.getId())) {
      return;
    }
    throw new ApiException(403, "无权限");
  }

  private static PaymentRecordResponse toResponse(PaymentRecord r) {
    return new PaymentRecordResponse(
        r.getId(),
        r.getRegistrationId(),
        r.getPayerUserId(),
        r.getPayeeUserId(),
        r.getAmount(),
        r.getPaymentScreenshotUrl(),
        r.getPayerConfirmedAt(),
        r.getPayeeConfirmedAt(),
        r.getStatus(),
        r.getCreatedAt());
  }
}

