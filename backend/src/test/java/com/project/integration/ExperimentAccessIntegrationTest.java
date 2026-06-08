package com.project.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.common.exception.ApiException;
import com.project.experiment.ExperimentConstants;
import com.project.experiment.dto.ExperimentCreateRequest;
import com.project.experiment.dto.ExperimentQueryRequest;
import com.project.experiment.dto.ExperimentResponse;
import com.project.registration.dto.RegistrationResponse;
import com.project.user.UserRoles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExperimentAccessIntegrationTest extends IntegrationTestSupport {

  @Test
  void researcherQueryOnlyReturnsOwnExperiments() {
    var researcherA = createUser("researcher-a", UserRoles.RESEARCHER);
    var researcherB = createUser("researcher-b", UserRoles.RESEARCHER);

    ExperimentResponse ownExperiment =
        createPublishedExperiment(researcherA.getUsername(), "研究者 A 的实验");
    createPublishedExperiment(researcherB.getUsername(), "研究者 B 的实验");

    ExperimentQueryRequest query = new ExperimentQueryRequest();
    query.setOrganizerId(researcherB.getId());

    Page<ExperimentResponse> result = experimentService.query(query, 0, 10, researcherA.getUsername());

    assertThat(result.getContent()).extracting(ExperimentResponse::getId).containsExactly(ownExperiment.getId());
    assertThat(result.getContent())
        .allSatisfy(item -> assertThat(item.getOrganizerId()).isEqualTo(researcherA.getId()));
  }

  @Test
  void researcherCannotGetAnotherResearchersExperimentById() {
    var researcherA = createUser("researcher-c", UserRoles.RESEARCHER);
    var researcherB = createUser("researcher-d", UserRoles.RESEARCHER);

    ExperimentResponse experiment =
        createPublishedExperiment(researcherB.getUsername(), "研究者 B 的详情实验");

    assertThatThrownBy(() -> experimentService.getById(experiment.getId(), researcherA.getUsername()))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("研究者只能查看自己创建的实验");
  }

  @Test
  void participantLimitIsStoredAndEnforcedOnApproval() {
    var researcher = createUser("limit-researcher", UserRoles.RESEARCHER);
    var subjectA = createUser("limit-subject-a", UserRoles.SUBJECT);
    var subjectB = createUser("limit-subject-b", UserRoles.SUBJECT);

    ExperimentCreateRequest request = new ExperimentCreateRequest();
    request.setTitle("限额实验");
    request.setDescription("用于验证人数上限");
    request.setLocation("心理学院 201 室");
    request.setParticipantLimit(1);
    request.setStartTime(LocalDateTime.now().plusDays(1));
    request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
    request.setEthicsApprovalNo("IRB-LIMIT-2026-001");
    request.setRiskLevel(ExperimentConstants.RISK_LOW);
    request.setPaymentAmount(new BigDecimal("50.00"));
    request.setPaymentMethod(ExperimentConstants.PAYMENT_OFFLINE);
    request.setPaymentDescription("实验结束后支付");

    ExperimentResponse created = experimentService.create(request, researcher.getUsername());
    experimentService.publish(created.getId(), researcher.getUsername());

    ExperimentResponse stored = experimentService.getById(created.getId(), researcher.getUsername());
    assertThat(stored.getParticipantLimit()).isEqualTo(1);

    RegistrationResponse registrationA =
        registrationService.apply(subjectA.getUsername(), created.getId());
    registrationService.approve(researcher.getUsername(), registrationA.getId());

    RegistrationResponse registrationB =
        registrationService.apply(subjectB.getUsername(), created.getId());

    assertThatThrownBy(() -> registrationService.approve(researcher.getUsername(), registrationB.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("实验人数已满");
  }
}
