package com.project.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.review.ReviewConstants;
import com.project.user.UserRoles;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppealFlowIntegrationTest extends IntegrationTestSupport {

  @Test
  void approveAppealShouldCreateNotification() {
    var researcher = createUser("appeal-researcher", UserRoles.RESEARCHER);
    var subject = createUser("appeal-subject", UserRoles.SUBJECT);
    var admin = createUser("appeal-admin", UserRoles.ADMIN);

    var experiment = createPublishedExperiment(researcher.getUsername(), "申诉集成实验");
    var registration = applyAndApproveRegistration(subject.getUsername(), researcher.getUsername(), experiment.getId());
    signInAndComplete(researcher.getUsername(), registration.getId());
    var review = submitReview(
        subject.getUsername(),
        registration.getId(),
        ReviewConstants.SUBJECT_TO_RESEARCHER,
        2,
        "实验组织一般，申诉后再处理",
        true);

    var appeal = submitAppeal(
        subject.getUsername(),
        "LOW_RATING",
        review.getId(),
        "评分过低，且与实际体验不符");
    var reviewed = reviewAppeal(appeal.getId(), admin.getUsername(), "APPROVED", "证据充分，同意撤销原评价影响");

    assertThat(reviewed.getStatus()).isEqualTo("APPROVED");
    assertThat(reviewed.getReviewerId()).isEqualTo(admin.getId());
    assertThat(reviewed.getReviewComment()).contains("撤销原评价影响");
    assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(subject.getId()))
        .hasSize(1)
        .first()
        .extracting("title", "type")
        .containsExactly("您的申诉已通过", "APPEAL_PROCESSED");
  }

  @Test
  void rejectAppealShouldCreateRejectedNotification() {
    var researcher = createUser("appeal-researcher-2", UserRoles.RESEARCHER);
    var subject = createUser("appeal-subject-2", UserRoles.SUBJECT);
    var admin = createUser("appeal-admin-2", UserRoles.ADMIN);

    var experiment = createPublishedExperiment(researcher.getUsername(), "申诉集成实验-拒绝分支");
    var registration = applyAndApproveRegistration(subject.getUsername(), researcher.getUsername(), experiment.getId());
    signInAndComplete(researcher.getUsername(), registration.getId());
    var review = submitReview(
        subject.getUsername(),
        registration.getId(),
        ReviewConstants.SUBJECT_TO_RESEARCHER,
        1,
        "体验一般，希望重新审核",
        false);

    var appeal = submitAppeal(
        subject.getUsername(),
        "LOW_RATING",
        review.getId(),
        "我认为评分没有体现真实情况");
    var reviewed = reviewAppeal(appeal.getId(), admin.getUsername(), "REJECTED", "证据不足，维持原结果");

    assertThat(reviewed.getStatus()).isEqualTo("REJECTED");
    assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(subject.getId()))
        .hasSize(1)
        .first()
        .extracting("title")
        .isEqualTo("您的申诉已被拒绝");
  }
}