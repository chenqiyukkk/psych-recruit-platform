package com.project.integration;

import com.project.appeal.dto.AppealCreateRequest;
import com.project.appeal.dto.AppealReviewRequest;
import com.project.appeal.dto.AppealResponse;
import com.project.appeal.service.AppealService;
import com.project.experiment.ExperimentConstants;
import com.project.experiment.dto.ExperimentCreateRequest;
import com.project.experiment.dto.ExperimentResponse;
import com.project.experiment.dto.ExperimentTagRequest;
import com.project.experiment.repo.ExperimentRepository;
import com.project.experiment.service.ExperimentService;
import com.project.notification.repo.NotificationRepository;
import com.project.payment.PaymentConstants;
import com.project.payment.dto.PaymentCodeCreateRequest;
import com.project.payment.dto.PaymentCodeResponse;
import com.project.payment.dto.PaymentConfirmPayerRequest;
import com.project.payment.dto.PaymentConfirmPayeeRequest;
import com.project.payment.dto.PaymentDisputeRequest;
import com.project.payment.dto.PaymentRecordResponse;
import com.project.payment.repo.PaymentRecordRepository;
import com.project.payment.service.PaymentService;
import com.project.registration.dto.RegistrationResponse;
import com.project.registration.repo.RegistrationRepository;
import com.project.registration.service.RegistrationService;
import com.project.review.ReviewConstants;
import com.project.review.dto.ReviewCreateRequest;
import com.project.review.dto.ReviewResponse;
import com.project.review.repo.ReviewRepository;
import com.project.review.service.ReviewService;
import com.project.signin.service.SignInService;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

abstract class IntegrationTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected ExperimentRepository experimentRepository;
  @Autowired protected RegistrationRepository registrationRepository;
  @Autowired protected ReviewRepository reviewRepository;
  @Autowired protected NotificationRepository notificationRepository;
  @Autowired protected PaymentRecordRepository paymentRecordRepository;
  @Autowired protected ExperimentService experimentService;
  @Autowired protected RegistrationService registrationService;
  @Autowired protected SignInService signInService;
  @Autowired protected PaymentService paymentService;
  @Autowired protected ReviewService reviewService;
  @Autowired protected AppealService appealService;

  protected User createUser(String username, String role) {
    return createUser(username, role, 100);
  }

  protected User createUser(String username, String role, int reputationScore) {
    User user = new User();
    user.setUsername(username);
    user.setPassword("password-" + username);
    user.setPhone("1380000" + Math.abs(username.hashCode() % 10000));
    user.setEmail(username + "@example.com");
    user.setRole(role);
    user.setReputationScore(reputationScore);
    user.setCreatedAt(LocalDateTime.now());
    return userRepository.save(user);
  }

  protected ExperimentResponse createPublishedExperiment(String organizerUsername, String title) {
    ExperimentCreateRequest request = new ExperimentCreateRequest();
    request.setTitle(title);
    request.setDescription("用于集成测试的实验");
    request.setLocation("心理学院 101 室");
    request.setStartTime(LocalDateTime.now().plusDays(1));
    request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
    request.setEthicsApprovalNo("IRB-TEST-2026-001");
    request.setRiskLevel(ExperimentConstants.RISK_LOW);
    request.setPaymentAmount(new BigDecimal("80.00"));
    request.setPaymentMethod(ExperimentConstants.PAYMENT_OFFLINE);
    request.setPaymentDescription("实验结束后现场支付");
    request.setScreeningCriteria("{\"ageRange\":[18,25],\"gender\":\"不限\"}");
    request.setExcludeTags("[\"fMRI\"]");
    ExperimentTagRequest tag = new ExperimentTagRequest();
    tag.setTagName("认知类");
    tag.setCoolingDays(30);
    request.setTags(List.of(tag));

    ExperimentResponse created = experimentService.create(request, organizerUsername);
    experimentService.publish(created.getId(), organizerUsername);
    return experimentService.getById(created.getId());
  }

  protected RegistrationResponse applyAndApproveRegistration(
      String subjectUsername, String researcherUsername, Long experimentId) {
    RegistrationResponse applied = registrationService.apply(subjectUsername, experimentId);
    return registrationService.approve(researcherUsername, applied.getId());
  }

  protected RegistrationResponse signInAndComplete(String researcherUsername, Long registrationId) {
    signInService.signIn(researcherUsername, registrationId);
    return signInService.complete(researcherUsername, registrationId);
  }

  protected PaymentCodeResponse uploadPaymentCode(
      String username, String paymentType, String qrCodeUrl, boolean isDefault) {
    PaymentCodeCreateRequest request = new PaymentCodeCreateRequest();
    request.setPaymentType(paymentType);
    request.setQrCodeUrl(qrCodeUrl);
    request.setIsDefault(isDefault);
    return paymentService.createPaymentCode(username, request);
  }

  protected PaymentRecordResponse confirmPayer(
      String researcherUsername,
      Long registrationId,
      Long payeeUserId,
      BigDecimal amount,
      String screenshotUrl) {
    PaymentConfirmPayerRequest request = new PaymentConfirmPayerRequest();
    request.setRegistrationId(registrationId);
    request.setPayeeUserId(payeeUserId);
    request.setAmount(amount);
    request.setPaymentScreenshotUrl(screenshotUrl);
    return paymentService.confirmPayer(researcherUsername, request);
  }

  protected PaymentRecordResponse confirmPayee(String username, Long registrationId) {
    PaymentConfirmPayeeRequest request = new PaymentConfirmPayeeRequest();
    request.setRegistrationId(registrationId);
    return paymentService.confirmPayee(username, request);
  }

  protected PaymentRecordResponse disputePayment(String username, Long registrationId) {
    PaymentDisputeRequest request = new PaymentDisputeRequest();
    request.setRegistrationId(registrationId);
    return paymentService.dispute(username, request);
  }

  protected ReviewResponse submitReview(
      String username, Long registrationId, String reviewType, int rating, String comment, boolean anonymous) {
    ReviewCreateRequest request = new ReviewCreateRequest();
    request.setReviewType(reviewType);
    request.setRating(rating);
    request.setCommunicationScore(rating);
    request.setProfessionalismScore(rating);
    request.setPunctualityScore(rating);
    request.setComment(comment);
    request.setIsAnonymous(anonymous);
    return reviewService.create(username, registrationId, request);
  }

  protected AppealResponse submitAppeal(
      String username, String appealType, Long targetId, String reason) {
    AppealCreateRequest request = new AppealCreateRequest();
    request.setAppealType(appealType);
    request.setTargetId(targetId);
    request.setReason(reason);
    request.setEvidenceUrls("[\"https://example.com/evidence.png\"]");
    return appealService.create(getUserId(username), request);
  }

  protected AppealResponse reviewAppeal(
      Long appealId, String reviewerUsername, String decision, String reviewComment) {
    AppealReviewRequest request = new AppealReviewRequest();
    request.setDecision(decision);
    request.setReviewComment(reviewComment);
    return appealService.review(appealId, getUserId(reviewerUsername), request);
  }

  protected Long getUserId(String username) {
    return userRepository.findByUsername(username).orElseThrow().getId();
  }
}
