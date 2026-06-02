package com.project.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.payment.PaymentConstants;
import com.project.review.ReviewConstants;
import com.project.user.UserRoles;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ActivityFlowIntegrationTest extends IntegrationTestSupport {

  @Test
  void completeRecruitmentToEvaluationFlow() {
    var researcher = createUser("activity-researcher", UserRoles.RESEARCHER);
    var subject = createUser("activity-subject", UserRoles.SUBJECT, 92);

    var experiment = createPublishedExperiment(researcher.getUsername(), "集成测试实验");
    var applied = registrationService.apply(subject.getUsername(), experiment.getId());
    var approved = registrationService.approve(researcher.getUsername(), applied.getId());
    var signed = signInService.signIn(researcher.getUsername(), applied.getId());
    var completed = signInService.complete(researcher.getUsername(), applied.getId());

    var paymentRecord = confirmPayer(
        researcher.getUsername(),
        applied.getId(),
        subject.getId(),
        new BigDecimal("80.00"),
        "https://example.com/payment-proof.png");
    var confirmed = confirmPayee(subject.getUsername(), applied.getId());

    var subjectReview = submitReview(
        subject.getUsername(),
        applied.getId(),
        ReviewConstants.SUBJECT_TO_RESEARCHER,
        5,
        "研究者沟通清晰，实验组织规范",
        true);
    var researcherReview = submitReview(
        researcher.getUsername(),
        applied.getId(),
        ReviewConstants.RESEARCHER_TO_SUBJECT,
        4,
        "被试配合度高，签到准时",
        false);

    assertThat(experiment.getStatus()).isEqualTo("PUBLISHED");
    assertThat(approved.getStatus()).isEqualTo("APPROVED");
    assertThat(registrationRepository.findById(applied.getId()).orElseThrow().getStatus()).isEqualTo("APPROVED");
    assertThat(approved.getReviewedAt()).isNotNull();
    assertThat(signed.getSignInTime()).isNotNull();
    assertThat(completed.getIsCompleted()).isTrue();
    assertThat(paymentRecord.getStatus()).isEqualTo(PaymentConstants.STATUS_PAID);
    assertThat(confirmed.getStatus()).isEqualTo(PaymentConstants.STATUS_CONFIRMED);
    assertThat(subjectReview.getReviewType()).isEqualTo(ReviewConstants.SUBJECT_TO_RESEARCHER);
    assertThat(researcherReview.getReviewType()).isEqualTo(ReviewConstants.RESEARCHER_TO_SUBJECT);
    assertThat(reviewRepository.findByReviewerIdOrderByCreatedAtDesc(subject.getId())).hasSize(1);
    assertThat(reviewRepository.findByReviewerIdOrderByCreatedAtDesc(researcher.getId())).hasSize(1);
  }
}
