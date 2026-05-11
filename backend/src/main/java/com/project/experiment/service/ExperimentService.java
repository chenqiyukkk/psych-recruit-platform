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
import com.project.user.UserRoles;
import com.project.user.repo.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

    validateCreateOrUpdate(request.getRiskLevel(), request.getPaymentMethod(), request.getStartTime(), request.getEndTime(),
        request.getScreeningCriteria(), request.getExcludeTags());

    Experiment experiment = new Experiment();
    experiment.setTitle(request.getTitle());
    experiment.setDescription(request.getDescription());
    experiment.setLocation(request.getLocation());
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
    return getById(saved.getId());
  }

  @Transactional
  public ExperimentResponse update(
      Long id, ExperimentUpdateRequest request, String operatorUsername) {
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    assertOperatorCanManage(experiment, operatorUsername);

    if (!ExperimentConstants.STATUS_DRAFT.equals(experiment.getStatus())) {
      throw new ApiException(400, "仅草稿状态可编辑");
    }

    validateCreateOrUpdate(request.getRiskLevel(), request.getPaymentMethod(), request.getStartTime(), request.getEndTime(),
        request.getScreeningCriteria(), request.getExcludeTags());

    if (request.getTitle() != null) {
      experiment.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      experiment.setDescription(request.getDescription());
    }
    if (request.getLocation() != null) {
      experiment.setLocation(request.getLocation());
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

    return getById(id);
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

  public ExperimentResponse getById(Long id) {
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    List<ExperimentTagResponse> tags =
        experimentTagRepository.findByExperimentId(id).stream()
            .map(t -> new ExperimentTagResponse(t.getId(), t.getTagName(), t.getCoolingDays()))
            .collect(Collectors.toList());
    return toResponse(experiment, tags);
  }

  public Page<ExperimentResponse> query(ExperimentQueryRequest query, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Specification<Experiment> spec = buildSpec(query);
    return experimentRepository.findAll(spec, pageable).map(e -> toResponse(e, Collections.emptyList()));
  }

  @Transactional
  public void publish(Long id, String operatorUsername) {
    Experiment experiment = experimentRepository.findById(id).orElseThrow(() -> new ApiException(404, "实验不存在"));
    assertOperatorCanManage(experiment, operatorUsername);
    if (!ExperimentConstants.STATUS_DRAFT.equals(experiment.getStatus())) {
      throw new ApiException(400, "仅草稿状态可发布");
    }
    experiment.setStatus(ExperimentConstants.STATUS_PUBLISHED);
    experiment.setUpdatedAt(LocalDateTime.now());
    experimentRepository.save(experiment);
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
    var operator =
        userRepository.findByUsername(operatorUsername).orElseThrow(() -> new ApiException(401, "未登录"));
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

  private void validateCreateOrUpdate(
      String riskLevel,
      String paymentMethod,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String screeningCriteriaJson,
      String excludeTagsJson) {
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

  private Specification<Experiment> buildSpec(ExperimentQueryRequest q) {
    return (root, query, cb) -> {
      var predicates = new java.util.ArrayList<javax.persistence.criteria.Predicate>();
      if (q == null) {
        return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
      }
      if (StringUtils.hasText(q.getKeyword())) {
        String like = "%" + q.getKeyword().trim() + "%";
        predicates.add(cb.or(cb.like(root.get("title"), like), cb.like(root.get("description"), like)));
      }
      if (StringUtils.hasText(q.getStatus())) {
        predicates.add(cb.equal(root.get("status"), q.getStatus().trim()));
      }
      if (StringUtils.hasText(q.getRiskLevel())) {
        predicates.add(cb.equal(root.get("riskLevel"), q.getRiskLevel().trim()));
      }
      if (StringUtils.hasText(q.getPaymentMethod())) {
        predicates.add(cb.equal(root.get("paymentMethod"), q.getPaymentMethod().trim()));
      }
      if (q.getOrganizerId() != null) {
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

  private static ExperimentResponse toResponse(Experiment e, List<ExperimentTagResponse> tags) {
    return new ExperimentResponse(
        e.getId(),
        e.getTitle(),
        e.getDescription(),
        e.getLocation(),
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
}
