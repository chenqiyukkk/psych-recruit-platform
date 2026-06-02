package com.project.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.payment.PaymentConstants;
import com.project.user.UserRoles;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentFlowIntegrationTest extends IntegrationTestSupport {

  @Test
  void confirmAndDisputePaymentFlow() {
    var researcher = createUser("payment-researcher", UserRoles.RESEARCHER);
    var subject = createUser("payment-subject", UserRoles.SUBJECT);
    var subject2 = createUser("payment-subject-2", UserRoles.SUBJECT);

    var experiment = createPublishedExperiment(researcher.getUsername(), "支付集成实验");
    var firstRegistration = applyAndApproveRegistration(subject.getUsername(), researcher.getUsername(), experiment.getId());
    var secondRegistration = applyAndApproveRegistration(subject2.getUsername(), researcher.getUsername(), experiment.getId());

    uploadPaymentCode(subject.getUsername(), PaymentConstants.CODE_WECHAT, "https://example.com/wechat.png", true);
    uploadPaymentCode(subject.getUsername(), PaymentConstants.CODE_ALIPAY, "https://example.com/alipay.png", true);

    List<com.project.payment.dto.PaymentCodeResponse> codes = paymentService.myPaymentCodes(subject.getUsername());
    assertThat(codes).hasSize(2);
    assertThat(codes.get(0).getPaymentType()).isEqualTo(PaymentConstants.CODE_ALIPAY);
    assertThat(codes.get(0).getIsDefault()).isTrue();
    assertThat(codes.get(1).getPaymentType()).isEqualTo(PaymentConstants.CODE_WECHAT);
    assertThat(codes.get(1).getIsDefault()).isFalse();

    var payerConfirmed = confirmPayer(
        researcher.getUsername(),
        firstRegistration.getId(),
        subject.getId(),
        new BigDecimal("80.00"),
        "https://example.com/payment-proof-1.png");
    var payeeConfirmed = confirmPayee(subject.getUsername(), firstRegistration.getId());

    var disputed = confirmPayer(
        researcher.getUsername(),
        secondRegistration.getId(),
        subject2.getId(),
        new BigDecimal("80.00"),
        "https://example.com/payment-proof-2.png");
    var disputedStatus = disputePayment(subject2.getUsername(), secondRegistration.getId());

    assertThat(payerConfirmed.getStatus()).isEqualTo(PaymentConstants.STATUS_PAID);
    assertThat(payeeConfirmed.getStatus()).isEqualTo(PaymentConstants.STATUS_CONFIRMED);
    assertThat(disputed.getStatus()).isEqualTo(PaymentConstants.STATUS_PAID);
    assertThat(disputedStatus.getStatus()).isEqualTo(PaymentConstants.STATUS_DISPUTED);
    assertThat(paymentRecordRepository.findByRegistrationId(firstRegistration.getId()))
        .get()
        .extracting("status")
        .isEqualTo(PaymentConstants.STATUS_CONFIRMED);
    assertThat(paymentRecordRepository.findByRegistrationId(secondRegistration.getId()))
        .get()
        .extracting("status")
        .isEqualTo(PaymentConstants.STATUS_DISPUTED);
  }
}
