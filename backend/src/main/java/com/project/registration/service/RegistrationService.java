package com.project.registration.service;


import com.project.common.exception.ApiException;
import com.project.experiment.ExperimentConstants;
import com.project.experiment.entity.Experiment;
import com.project.experiment.entity.ExperimentTag;
import com.project.experiment.repo.ExperimentRepository;
import com.project.experiment.repo.ExperimentTagRepository;
import com.project.notification.service.NotificationService;
import com.project.registration.RegistrationConstants;
import com.project.registration.dto.RegistrationResponse;
import com.project.registration.entity.Registration;
import com.project.registration.repo.RegistrationRepository;
import com.project.user.UserRoles;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;

    private final ExperimentRepository experimentRepository;

    private final ExperimentTagRepository experimentTagRepository;

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public RegistrationResponse apply(String username,Long experimentId){
        User user = getUserByUsername(username);

        if(!UserRoles.SUBJECT.equals((user.getRole()))){
            throw new ApiException(403,"仅被试可以报名实验");
        }

        if(user.getReputationScore() < 60){
            throw new ApiException(400,"信誉分不足（当前" + user.getReputationScore() + "分，需要至少60分），无法报名实验");
        }

        Experiment experiment = getExperimentById(experimentId);

        if(!ExperimentConstants.STATUS_PUBLISHED.equals(experiment.getStatus())
            && !ExperimentConstants.STATUS_RECRUITING.equals(experiment.getStatus())){
            throw new ApiException(400,"当前实验不可报名");
        }

        if(registrationRepository.existsByExperimentIdAndUserId(experimentId,user.getId())){
            throw new ApiException(400,"不能重复报名同一个实验");
        }

        // 筛选条件校验
        validateScreeningCriteria(user, experiment);

        // 检查实验人数上限
        Integer participantLimit = experiment.getParticipantLimit();
        if (participantLimit != null) {
            long occupiedSlots = registrationRepository.countByExperimentIdAndStatusIn(
                    experiment.getId(),
                    Set.of(RegistrationConstants.STATUS_APPROVED));
            if (occupiedSlots >= participantLimit) {
                throw new ApiException(400, "实验人数已满，无法报名");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        validateTagCoolingPeriod(user.getId(), experiment, now);

        Registration registration = new Registration();
        registration.setExperimentId(experimentId);
        registration.setUserId(user.getId());
        registration.setStatus(RegistrationConstants.STATUS_PENDING);
        registration.setAppliedAt(now);
        registration.setIsCompleted(false);
        registration.setCreatedAt(now);
        registration.setUpdatedAt(now);

        return toResponse(registrationRepository.save(registration));
    }

    public List<RegistrationResponse> getMyRegistrations(String username){
        User user = getUserByUsername(username);

        return registrationRepository.findByUserIdOrderByAppliedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<RegistrationResponse> getExperimentRegistrations(String username,Long experimentId){
        Experiment experiment = getExperimentById(experimentId);
        assertCanManageExperiment(username,experiment);

        return registrationRepository.findByExperimentIdOrderByAppliedAtDesc(experimentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RegistrationResponse approve(String username,Long registrationId){
        Registration registration = getRegistrationById(registrationId);
        Experiment experiment = getExperimentById(registration.getExperimentId());
        assertCanManageExperiment(username,experiment);

        if(!RegistrationConstants.STATUS_PENDING.equals(registration.getStatus())){
            throw new ApiException(400,"只有待审核报名可以通过");
        }

        Integer participantLimit = experiment.getParticipantLimit();
        if (participantLimit != null) {
            long occupiedSlots =
                    registrationRepository.countByExperimentIdAndStatusIn(
                            experiment.getId(),
                            Set.of(RegistrationConstants.STATUS_APPROVED));
            if (occupiedSlots >= participantLimit) {
                throw new ApiException(400, "实验人数已满");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        registration.setStatus(RegistrationConstants.STATUS_APPROVED);
        registration.setReviewedAt(now);
        registration.setUpdatedAt(now);
        registrationRepository.save(registration);

        // 审核通过后检查是否满员
        if (participantLimit != null) {
            long newOccupiedSlots = registrationRepository.countByExperimentIdAndStatusIn(
                    experiment.getId(),
                    Set.of(RegistrationConstants.STATUS_APPROVED));
            if (newOccupiedSlots >= participantLimit) {
                experiment.setStatus(ExperimentConstants.STATUS_FULL);
                experimentRepository.save(experiment);
            }
        }

        // 通知被试报名已通过
        notificationService.send(registration.getUserId(),
            "报名已通过", "你报名参加的实验「" + experiment.getTitle() + "」已通过审核，请按时参加。",
            "REGISTRATION_APPROVED", "registration", registration.getId());

        return toResponse(registration);
    }

    @Transactional
    public RegistrationResponse reject(String username,Long registrationId){
        Registration registration = getRegistrationById(registrationId);
        Experiment experiment = getExperimentById(registration.getExperimentId());
        assertCanManageExperiment(username,experiment);

        if(!RegistrationConstants.STATUS_PENDING.equals(registration.getStatus())){
            throw new ApiException(400,"只有待审核报名可以拒绝");
        }

        LocalDateTime now =LocalDateTime.now();
        registration.setStatus(RegistrationConstants.STATUS_REJECTED);
        registration.setReviewedAt(now);
        registration.setUpdatedAt(now);
        registrationRepository.save(registration);

        // 通知被试报名被拒绝
        notificationService.send(registration.getUserId(),
            "报名未通过", "你报名参加的实验「" + experiment.getTitle() + "」未通过审核。",
            "REGISTRATION_REJECTED", "registration", registration.getId());

        return toResponse(registration);
    }


    @Transactional
    public RegistrationResponse cancel(String username,Long registrationId){
        User user = getUserByUsername(username);
        Registration registration = getRegistrationById(registrationId);

        if(!Objects.equals(registration.getUserId(),user.getId())){
            throw new ApiException(403,"只能取消自己的报名");
        }

        if(RegistrationConstants.STATUS_REJECTED.equals(registration.getStatus())
        || RegistrationConstants.STATUS_CANCELLED.equals(registration.getStatus())){
            throw new ApiException(400,"当前状态不可取消");
        }

        boolean wasApproved = RegistrationConstants.STATUS_APPROVED.equals(registration.getStatus());

        registration.setStatus(RegistrationConstants.STATUS_CANCELLED);
        registration.setUpdatedAt(LocalDateTime.now());
        registrationRepository.save(registration);

        // 取消已通过报名后，若实验满员则恢复为招募中
        if (wasApproved) {
            revertExperimentFullStatus(registration.getExperimentId());
        }

        return toResponse(registration);
    }

    private User getUserByUsername(String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(401,"未登录或用户不存在"));
    }

    private Experiment getExperimentById(Long experimentId){
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ApiException(404,"实验不存在"));
    }

    private Registration getRegistrationById(Long registrationId){
        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ApiException(401,"报名记录不存在"));
    }

    private void validateScreeningCriteria(User user, Experiment experiment) {
        String criteriaJson = experiment.getScreeningCriteria();
        if (criteriaJson == null || criteriaJson.trim().isEmpty()) {
            return;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(criteriaJson);

            // 支持 {"include": {...}} 和直接 {...} 两种格式
            com.fasterxml.jackson.databind.JsonNode include = root.has("include") ? root.get("include") : root;

            // 性别校验
            if (include.has("gender") && !include.get("gender").asText().equals("ANY")) {
                String requiredGender = include.get("gender").asText();
                String userGender = user.getGender();
                if (userGender == null || !userGender.equals(requiredGender)) {
                    throw new ApiException(400, "你的性别不符合本次实验要求");
                }
            }

            // 年龄校验
            if (include.has("age_range")) {
                com.fasterxml.jackson.databind.JsonNode ageRange = include.get("age_range");
                int minAge = ageRange.get(0).asInt(0);
                int maxAge = ageRange.size() > 1 ? ageRange.get(1).asInt(99) : 99;
                String userAgeGroup = user.getAgeGroup();
                if (userAgeGroup != null && !userAgeGroup.isEmpty()) {
                    String[] parts = userAgeGroup.split("-");
                    int userAge = Integer.parseInt(parts[0].trim());
                    if (userAge < minAge || userAge > maxAge) {
                        throw new ApiException(400, "你的年龄不符合本次实验要求");
                    }
                }
            }

            // 专业校验（空数组 = 不限，跳过）
            if (include.has("major_categories") && include.get("major_categories").isArray()
                && include.get("major_categories").size() > 0) {
                boolean allAny = true;
                for (com.fasterxml.jackson.databind.JsonNode mc : include.get("major_categories")) {
                    if (!"不限".equals(mc.asText())) { allAny = false; break; }
                }
                if (allAny) {
                    // 全选"不限"，跳过专业校验
                } else {
                String userMajor = user.getMajorCategory();
                if (userMajor == null || userMajor.isEmpty()) {
                    throw new ApiException(400, "请先在个人中心填写你的专业类别");
                }
                boolean matched = false;
                for (com.fasterxml.jackson.databind.JsonNode mc : include.get("major_categories")) {
                    if (userMajor.equals(mc.asText())) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    throw new ApiException(400, "你的专业类别不符合本次实验要求");
                }
                }
            }

            // 利手校验
            if (include.has("handedness") && !include.get("handedness").asText().equals("ANY")) {
                String requiredHand = include.get("handedness").asText();
                String userHand = user.getHandedness();
                if (userHand == null || !userHand.equals(requiredHand)) {
                    throw new ApiException(400, "你的利手不符合本次实验要求");
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            // JSON 解析失败，忽略筛选（不阻塞报名）
        }
    }

    private void validateTagCoolingPeriod(Long userId, Experiment experiment, LocalDateTime now) {
        List<ExperimentTag> currentTags = experimentTagRepository.findByExperimentId(experiment.getId());
        if (currentTags.isEmpty()) {
            return;
        }

        for (Registration completedRegistration :
                registrationRepository.findByUserIdAndIsCompletedTrueOrderByAppliedAtDesc(userId)) {
            Optional<Experiment> historicalExperiment =
                    experimentRepository.findById(completedRegistration.getExperimentId());
            if (historicalExperiment.isEmpty()) {
                continue;
            }

            List<ExperimentTag> historicalTags =
                    experimentTagRepository.findByExperimentId(historicalExperiment.get().getId());
            for (ExperimentTag currentTag : currentTags) {
                if (currentTag.getCoolingDays() == null || currentTag.getCoolingDays() <= 0) {
                    continue;
                }
                boolean sameTag = historicalTags.stream()
                        .anyMatch(historicalTag -> Objects.equals(
                                normalizeTagName(historicalTag.getTagName()),
                                normalizeTagName(currentTag.getTagName())));
                if (!sameTag) {
                    continue;
                }

                LocalDateTime availableAt =
                        historicalExperiment.get().getEndTime().plusDays(currentTag.getCoolingDays());
                if (availableAt.isAfter(now)) {
                    long remainingDays = Duration.between(now, availableAt).toDays() + 1;
                    throw new ApiException(
                            400,
                            "您近期已参加过同类实验，请" + remainingDays + "天后再报名");
                }
            }
        }
    }

    private String normalizeTagName(String tagName) {
        return tagName == null ? "" : tagName.trim();
    }

    private void assertCanManageExperiment(String username,Experiment experiment){
        User operator = getUserByUsername(username);

        if(UserRoles.ADMIN.equals(operator.getRole())){
            return;
        }

        if(!UserRoles.RESEARCHER.equals(operator.getRole())){
            throw new ApiException(403,"无权限操作该实验报名");
        }

        if(!Objects.equals(experiment.getOrganizerId(),operator.getId())){
            throw new ApiException(403,"只能管理自己创建的实验报名");
        }
    }

    /**
     * 已通过报名被取消后，若实验处于 FULL 状态且名额已释放，恢复为 PUBLISHED。
     */
    private void revertExperimentFullStatus(Long experimentId) {
        Experiment experiment = getExperimentById(experimentId);
        if (!ExperimentConstants.STATUS_FULL.equals(experiment.getStatus())) {
            return;
        }
        Integer participantLimit = experiment.getParticipantLimit();
        if (participantLimit == null) {
            return;
        }
        long approvedCount = registrationRepository.countByExperimentIdAndStatusIn(
                experiment.getId(), Set.of(RegistrationConstants.STATUS_APPROVED));
        if (approvedCount < participantLimit) {
            experiment.setStatus(ExperimentConstants.STATUS_PUBLISHED);
            experiment.setUpdatedAt(LocalDateTime.now());
            experimentRepository.save(experiment);
        }
    }

    private RegistrationResponse toResponse(Registration registration){
        return new RegistrationResponse(
                registration.getId(),
                registration.getExperimentId(),
                registration.getUserId(),
                registration.getStatus(),
                registration.getAppliedAt(),
                registration.getReviewedAt(),
                registration.getSignInTime(),
                registration.getIsCompleted(),
                registration.getCreatedAt(),
                registration.getUpdatedAt()
        );
    }
}
