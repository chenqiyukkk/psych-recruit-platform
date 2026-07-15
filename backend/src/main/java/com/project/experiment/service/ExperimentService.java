package com.project.experiment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.ApiException;
import com.project.experiment.ExperimentConstants;
import com.project.experiment.dto.ExperimentCreateRequest;
import com.project.experiment.dto.ExperimentQueryRequest;
import com.project.experiment.dto.ExperimentResponse;
import com.project.experiment.dto.ExperimentTagRequest;
import com.project.experiment.dto.ExperimentTagResponse;
import com.project.experiment.dto.ExperimentUpdateRequest;
import com.project.experiment.entity.Experiment;
import com.project.experiment.entity.ExperimentTag;
import com.project.experiment.repo.ExperimentRepository;
import com.project.experiment.repo.ExperimentTagRepository;
import com.project.notification.service.NotificationService;
import com.project.registration.RegistrationConstants;
import com.project.registration.repo.RegistrationRepository;
import com.project.user.UserRoles;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ExperimentService {

  private final ExperimentRepository experimentRepository;
  private final ExperimentTagRepository experimentTagRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  private final RegistrationRepository registrationRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Transactional
  public ExperimentResponse create(ExperimentCreateRequest request, String organizerUsername) {
    var organizer =
        userRepository.findByUsername(organizerUsername).orElseThrow(() -> new ApiException(401, "未登录"));
    Long organizerId = organizer.getId();
    String organizerRole = organizer.getRole();
    if (!Objects.equals(organizerRole, UserRoles.RESEARCHER)
        && !Objects.equals(organizerRole, UserRoles.ADMIN)) {
      throw new ApiException(403, "仅研究者可创建实验");
    }

    validateCreateOrUpdate(
        request.getParticipantLimit(),
        request.getRiskLevel(),
        request.getPaymentMethod(),
        request.getStartTime(),
        request.getEndTime(),
        request.getScreeningCriteria(),
        request.getExcludeTags());

    Experiment experiment = new Experiment();
    experiment.setTitle(request.getTitle());
    experiment.setDescription(request.getDescription());
    experiment.setLocation(request.getLocation());
    experiment.setParticipantLimit(request.getParticipantLimit());
    experiment.setStartTime(request.getStartTime());
    experiment.setEndTime(request.getEndTime());
    experiment.setEthicsApprovalNo(request.getEthicsApprovalNo());
    experiment.setRiskLevel(request.getRiskLevel());
    experiment.setPaymentAmount(request.getPaymentAmount());
    experiment.setPaymentMethod(request.getPaymentMethod());
    experiment.setPaymentDescription(request.getPaymentDescription());
    experiment.setScreeningCriteria(trimToNull(request.getScreeningCriteria()));
    experiment.setExcludeTags(trimToNull(request.getExcludeTags()));
    experiment.setStatus(ExperimentConstants.STATUS_DRAFT);
    experiment.setOrganizerId(organizerId);
    LocalDateTime now = LocalDateTime.now();
    experiment.setCreatedAt(now);
    experiment.setUpdatedAt(now);
    Experiment saved = experimentRepository.save(experiment);

    replaceTags(saved.getId(), request.getTags());
    return getById(saved.getId(), organizerUsername);
  }

  @Transactional
  public ExperimentResponse update(
      Long id, ExperimentUpdateRequest request, String operatorUsername) {
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    assertOperatorCanManage(experiment, operatorUsername);

    if (!ExperimentConstants.STATUS_DRAFT.equals(experiment.getStatus())) {
      throw new ApiException(400, "仅草稿状态可编辑");
    }

    validateCreateOrUpdate(
        request.getParticipantLimit(),
        request.getRiskLevel(),
        request.getPaymentMethod(),
        request.getStartTime(),
        request.getEndTime(),
        request.getScreeningCriteria(),
        request.getExcludeTags());

    if (request.getTitle() != null) {
      experiment.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      experiment.setDescription(request.getDescription());
    }
    if (request.getLocation() != null) {
      experiment.setLocation(request.getLocation());
    }
    if (request.getParticipantLimit() != null) {
      experiment.setParticipantLimit(request.getParticipantLimit());
    }
    if (request.getStartTime() != null) {
      experiment.setStartTime(request.getStartTime());
    }
    if (request.getEndTime() != null) {
      experiment.setEndTime(request.getEndTime());
    }
    if (request.getEthicsApprovalNo() != null) {
      experiment.setEthicsApprovalNo(request.getEthicsApprovalNo());
    }
    if (request.getRiskLevel() != null) {
      experiment.setRiskLevel(request.getRiskLevel());
    }
    if (request.getPaymentAmount() != null) {
      experiment.setPaymentAmount(request.getPaymentAmount());
    }
    if (request.getPaymentMethod() != null) {
      experiment.setPaymentMethod(request.getPaymentMethod());
    }
    if (request.getPaymentDescription() != null) {
      experiment.setPaymentDescription(request.getPaymentDescription());
    }
    if (request.getScreeningCriteria() != null) {
      experiment.setScreeningCriteria(trimToNull(request.getScreeningCriteria()));
    }
    if (request.getExcludeTags() != null) {
      experiment.setExcludeTags(trimToNull(request.getExcludeTags()));
    }
    experiment.setUpdatedAt(LocalDateTime.now());
    experimentRepository.save(experiment);

    if (request.getTags() != null) {
      replaceTags(id, request.getTags());
    }

    return getById(id, operatorUsername);
  }

  @Transactional
  public void delete(Long id, String operatorUsername) {
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    assertOperatorCanManage(experiment, operatorUsername);
    if (!ExperimentConstants.STATUS_DRAFT.equals(experiment.getStatus())) {
      throw new ApiException(400, "仅草稿状态可删除");
    }
    experimentTagRepository.deleteByExperimentId(id);
    experimentRepository.deleteById(id);
  }

  public ExperimentResponse getById(Long id, String requesterUsername) {
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    assertOperatorCanView(experiment, requesterUsername);
    List<ExperimentTagResponse> tags =
        experimentTagRepository.findByExperimentId(id).stream()
            .map(t -> new ExperimentTagResponse(t.getId(), t.getTagName(), t.getCoolingDays()))
            .collect(Collectors.toList());
    long approvedCount = registrationRepository.countByExperimentIdAndStatusIn(
        id, Set.of(RegistrationConstants.STATUS_APPROVED));
    return toResponse(experiment, tags, approvedCount);
  }

  public Page<ExperimentResponse> query(
      ExperimentQueryRequest query, int page, int size, String requesterUsername) {
    User requester = getUserByUsername(requesterUsername);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Specification<Experiment> spec = buildSpec(query, requester);
    Page<Experiment> experimentPage = experimentRepository.findAll(spec, pageable);

    // 批量加载标签，避免 N+1
    List<Long> ids = experimentPage.getContent().stream().map(Experiment::getId).collect(Collectors.toList());
    Map<Long, List<ExperimentTagResponse>> tagMap =
        experimentTagRepository.findByExperimentIdIn(ids).stream()
            .collect(Collectors.groupingBy(
                ExperimentTag::getExperimentId,
                Collectors.mapping(
                    t -> new ExperimentTagResponse(t.getId(), t.getTagName(), t.getCoolingDays()),
                    Collectors.toList())));

    // 批量加载已通过报名人数
    Map<Long, Long> approvedCountMap = new HashMap<>();
    if (!ids.isEmpty()) {
      List<Object[]> rows = registrationRepository.countApprovedByExperimentIds(
          ids, Set.of(RegistrationConstants.STATUS_APPROVED));
      for (Object[] row : rows) {
        approvedCountMap.put((Long) row[0], (Long) row[1]);
      }
    }

    return experimentPage.map(e -> toResponse(e,
        tagMap.getOrDefault(e.getId(), Collections.emptyList()),
        approvedCountMap.getOrDefault(e.getId(), 0L)));
  }

  @Transactional
  public void publish(Long id, String operatorUsername) {
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    assertOperatorCanManage(experiment, operatorUsername);
    // 研究者只能发布已通过审核的实验（PENDING_REVIEW 由管理员改为此状态后再发布）
    // 管理员可直接发布
    User operator = getUserByUsername(operatorUsername);
    if (UserRoles.RESEARCHER.equals(operator.getRole())) {
      if (!ExperimentConstants.STATUS_PENDING_REVIEW.equals(experiment.getStatus())) {
        throw new ApiException(400, "实验需先通过管理员审核才能发布");
      }
    } else if (UserRoles.ADMIN.equals(operator.getRole())) {
      // 管理员审批通过即发布
    }
    // 研究者评分过低（<2分且有至少3次评价）禁止发布
    User organizer = userRepository.findById(experiment.getOrganizerId()).orElse(null);
    if (organizer != null && organizer.getResearcherRating() != null
        && organizer.getResearcherRating().compareTo(new java.math.BigDecimal("2.0")) < 0
        && organizer.getTotalReviews() >= 3) {
      throw new ApiException(400, "研究者评分过低（" + organizer.getResearcherRating() + "分），需至少2分才能发布实验");
    }
    experiment.setStatus(ExperimentConstants.STATUS_PUBLISHED);
    experiment.setUpdatedAt(LocalDateTime.now());
    experimentRepository.save(experiment);
  }

  @Transactional
  public void submitForReview(Long id, String operatorUsername) {
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    assertOperatorCanManage(experiment, operatorUsername);
    if (!ExperimentConstants.STATUS_DRAFT.equals(experiment.getStatus())) {
      throw new ApiException(400, "仅草稿状态可提交审核");
    }
    experiment.setStatus(ExperimentConstants.STATUS_PENDING_REVIEW);
    experiment.setUpdatedAt(LocalDateTime.now());
    experimentRepository.save(experiment);
  }

  @Transactional
  public void approve(Long id, String operatorUsername) {
    User operator = getUserByUsername(operatorUsername);
    if (!UserRoles.ADMIN.equals(operator.getRole())) {
      throw new ApiException(403, "仅管理员可审批实验");
    }
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    if (!ExperimentConstants.STATUS_PENDING_REVIEW.equals(experiment.getStatus())) {
      throw new ApiException(400, "仅待审核状态可审批");
    }
    experiment.setStatus(ExperimentConstants.STATUS_PUBLISHED);
    experiment.setUpdatedAt(LocalDateTime.now());
    experimentRepository.save(experiment);

    // 通知研究者审核通过
    notificationService.send(experiment.getOrganizerId(),
        "实验审核通过", "你提交的实验「" + experiment.getTitle() + "」已通过管理员审核，可前往实验列表发布。",
        "EXPERIMENT_APPROVED", "experiment", experiment.getId());

  }

  @Transactional
  public void reject(Long id, String operatorUsername, String reason) {
    User operator = getUserByUsername(operatorUsername);
    if (!UserRoles.ADMIN.equals(operator.getRole())) {
      throw new ApiException(403, "仅管理员可驳回实验");
    }
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    if (!ExperimentConstants.STATUS_PENDING_REVIEW.equals(experiment.getStatus())) {
      throw new ApiException(400, "仅待审核状态可驳回");
    }
    experiment.setStatus(ExperimentConstants.STATUS_DRAFT);
    experiment.setReviewComment(reason != null ? reason.trim() : "管理员驳回了该实验，请修改后重新提交");
    experiment.setUpdatedAt(LocalDateTime.now());
    experimentRepository.save(experiment);

    // 通知研究者被驳回
    String rejectReason = experiment.getReviewComment();
    notificationService.send(experiment.getOrganizerId(),
        "实验审核被驳回", "你提交的实验「" + experiment.getTitle() + "」未通过审核。"
            + (rejectReason != null ? "原因：" + rejectReason : ""),
        "EXPERIMENT_REJECTED", "experiment", experiment.getId());
  }

  @Transactional
  public void cancel(Long id, String operatorUsername) {
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    assertOperatorCanManage(experiment, operatorUsername);
    if (ExperimentConstants.STATUS_COMPLETED.equals(experiment.getStatus())) {
      throw new ApiException(400, "已完成实验不可取消");
    }
    experiment.setStatus(ExperimentConstants.STATUS_DRAFT);
    experiment.setUpdatedAt(LocalDateTime.now());
    experimentRepository.save(experiment);
  }

  private void replaceTags(Long experimentId, List<ExperimentTagRequest> tags) {
    experimentTagRepository.deleteByExperimentId(experimentId);
    if (tags == null || tags.isEmpty()) {
      return;
    }
    List<ExperimentTag> entities =
        tags.stream()
            .filter(Objects::nonNull)
            .map(
                t -> {
                  ExperimentTag et = new ExperimentTag();
                  et.setExperimentId(experimentId);
                  et.setTagName(t.getTagName());
                  et.setCoolingDays(t.getCoolingDays());
                  return et;
                })
            .collect(Collectors.toList());
    experimentTagRepository.saveAll(entities);
  }

  private void assertOperatorCanManage(Experiment experiment, String operatorUsername) {
    var operator = getUserByUsername(operatorUsername);
    if (Objects.equals(operator.getRole(), UserRoles.ADMIN)) {
      return;
    }
    if (!Objects.equals(operator.getRole(), UserRoles.RESEARCHER)) {
      throw new ApiException(403, "无权限");
    }
    if (!Objects.equals(experiment.getOrganizerId(), operator.getId())) {
      throw new ApiException(403, "仅创建者可操作该实验");
    }
  }

  private void assertOperatorCanView(Experiment experiment, String requesterUsername) {
    User requester = getUserByUsername(requesterUsername);
    if (!Objects.equals(requester.getRole(), UserRoles.RESEARCHER)) {
      return;
    }
    if (!Objects.equals(experiment.getOrganizerId(), requester.getId())) {
      throw new ApiException(403, "研究者只能查看自己创建的实验");
    }
  }

  private void validateCreateOrUpdate(
      Integer participantLimit,
      String riskLevel,
      String paymentMethod,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String screeningCriteriaJson,
      String excludeTagsJson) {
    if (participantLimit != null && participantLimit < 1) {
      throw new ApiException(400, "participantLimit 必须大于 0");
    }
    if (riskLevel != null && !ExperimentConstants.RISK_LEVELS.contains(riskLevel)) {
      throw new ApiException(400, "riskLevel 不合法");
    }
    if (paymentMethod != null && !ExperimentConstants.PAYMENT_METHODS.contains(paymentMethod)) {
      throw new ApiException(400, "paymentMethod 不合法");
    }
    if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
      throw new ApiException(400, "endTime 不能早于 startTime");
    }
    if (screeningCriteriaJson != null) {
      ensureJson(screeningCriteriaJson, "screeningCriteria");
    }
    if (excludeTagsJson != null) {
      ensureJson(excludeTagsJson, "excludeTags");
    }
  }

  private void ensureJson(String json, String field) {
    String trimmed = json == null ? "" : json.trim();
    if (!StringUtils.hasText(trimmed)) {
      return;
    }
    try {
      objectMapper.readTree(trimmed);
    } catch (Exception e) {
      throw new ApiException(400, field + " 必须是合法 JSON");
    }
  }

  private Specification<Experiment> buildSpec(ExperimentQueryRequest q, User requester) {
    return (root, query, cb) -> {
      var predicates = new java.util.ArrayList<javax.persistence.criteria.Predicate>();
      if (q == null) {
        if (Objects.equals(requester.getRole(), UserRoles.RESEARCHER)) {
          predicates.add(cb.equal(root.get("organizerId"), requester.getId()));
        }
        return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
      }
      if (StringUtils.hasText(q.getKeyword())) {
        String like = "%" + q.getKeyword().trim() + "%";
        predicates.add(cb.or(cb.like(root.get("title"), like), cb.like(root.get("description"), like)));
      }
      if (StringUtils.hasText(q.getStatus())) {
        String[] statuses = q.getStatus().trim().split("\\s*,\\s*");
        if (statuses.length == 1) {
          predicates.add(cb.equal(root.get("status"), statuses[0]));
        } else {
          predicates.add(root.get("status").in((Object[]) statuses));
        }
      }
      if (StringUtils.hasText(q.getRiskLevel())) {
        predicates.add(cb.equal(root.get("riskLevel"), q.getRiskLevel().trim()));
      }
      if (StringUtils.hasText(q.getPaymentMethod())) {
        predicates.add(cb.equal(root.get("paymentMethod"), q.getPaymentMethod().trim()));
      }
      if (Objects.equals(requester.getRole(), UserRoles.RESEARCHER)) {
        predicates.add(cb.equal(root.get("organizerId"), requester.getId()));
      } else if (q.getOrganizerId() != null) {
        predicates.add(cb.equal(root.get("organizerId"), q.getOrganizerId()));
      }
      if (q.getStartFrom() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), q.getStartFrom()));
      }
      if (q.getEndTo() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("endTime"), q.getEndTo()));
      }
      return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
    };
  }

  private static ExperimentResponse toResponse(Experiment e, List<ExperimentTagResponse> tags, long approvedCount) {
    return new ExperimentResponse(
        e.getId(),
        e.getTitle(),
        e.getDescription(),
        e.getLocation(),
        e.getParticipantLimit(),
        approvedCount,
        e.getStartTime(),
        e.getEndTime(),
        e.getEthicsApprovalNo(),
        e.getRiskLevel(),
        e.getPaymentAmount(),
        e.getPaymentMethod(),
        e.getPaymentDescription(),
        e.getScreeningCriteria(),
        e.getExcludeTags(),
        e.getStatus(),
        e.getReviewComment(),
        e.getOrganizerId(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        tags);
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private User getUserByUsername(String username) {
    return userRepository.findByUsername(username).orElseThrow(() -> new ApiException(401, "未登录"));
  }
}
