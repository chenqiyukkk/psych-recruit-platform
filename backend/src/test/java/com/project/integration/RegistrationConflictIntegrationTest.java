package com.project.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.common.exception.ApiException;
import com.project.experiment.ExperimentConstants;
import com.project.experiment.dto.ExperimentCreateRequest;
import com.project.experiment.dto.ExperimentResponse;
import com.project.experiment.dto.ExperimentTagRequest;
import com.project.user.UserRoles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistrationConflictIntegrationTest extends IntegrationTestSupport {

  @Test
  void rejectsRegistrationWhenSameTagIsStillInCoolingPeriod() {
    var researcher = createUser("conflict-researcher", UserRoles.RESEARCHER);
    var subject = createUser("conflict-subject", UserRoles.SUBJECT);

    var previousExperiment =
        createPublishedExperiment(
            researcher.getUsername(),
            "已完成认知类实验",
            LocalDateTime.now().minusDays(5).minusHours(2),
            LocalDateTime.now().minusDays(5),
            "认知类",
            30);
    var previousRegistration =
        applyAndApproveRegistration(subject.getUsername(), researcher.getUsername(), previousExperiment.getId());
    signInAndComplete(researcher.getUsername(), previousRegistration.getId());

    var conflictingExperiment =
        createPublishedExperiment(
            researcher.getUsername(),
            "冷却期内认知类实验",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(2),
            "认知类",
            30);

    assertThatThrownBy(() -> registrationService.apply(subject.getUsername(), conflictingExperiment.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("您近期已参加过同类实验");
    assertThat(registrationRepository.findByUserIdOrderByAppliedAtDesc(subject.getId())).hasSize(1);
  }

  private ExperimentResponse createPublishedExperiment(
      String organizerUsername,
      String title,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String tagName,
      int coolingDays) {
    ExperimentCreateRequest request = new ExperimentCreateRequest();
    request.setTitle(title);
    request.setDescription("用于互斥规则集成测试的实验");
    request.setLocation("心理学院 102 室");
    request.setStartTime(startTime);
    request.setEndTime(endTime);
    request.setEthicsApprovalNo("IRB-CONFLICT-2026-001");
    request.setRiskLevel(ExperimentConstants.RISK_LOW);
    request.setPaymentAmount(new BigDecimal("60.00"));
    request.setPaymentMethod(ExperimentConstants.PAYMENT_OFFLINE);
    request.setPaymentDescription("实验结束后现场支付");
    request.setScreeningCriteria("{\"ageRange\":[18,25],\"gender\":\"不限\"}");
    request.setExcludeTags("[\"" + tagName + "\"]");
    ExperimentTagRequest tag = new ExperimentTagRequest();
    tag.setTagName(tagName);
    tag.setCoolingDays(coolingDays);
    request.setTags(List.of(tag));

    ExperimentResponse created = experimentService.create(request, organizerUsername);
    experimentService.publish(created.getId(), organizerUsername);
    return experimentService.getById(created.getId());
  }
}
