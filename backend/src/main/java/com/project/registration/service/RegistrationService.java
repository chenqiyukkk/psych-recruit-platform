package com.project.registration.service;


import com.project.common.exception.ApiException;
import com.project.experiment.ExperimentConstants;
import com.project.experiment.entity.Experiment;
import com.project.experiment.entity.ExperimentTag;
import com.project.experiment.repo.ExperimentRepository;
import com.project.experiment.repo.ExperimentTagRepository;
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

    @Transactional
    public RegistrationResponse apply(String username,Long experimentId){
        User user = getUserByUsername(username);

        if(!UserRoles.SUBJECT.equals((user.getRole()))){
            throw new ApiException(403,"仅被试可以报名实验");
        }

        Experiment experiment = getExperimentById(experimentId);

        if(!ExperimentConstants.STATUS_PUBLISHED.equals(experiment.getStatus())
            && !ExperimentConstants.STATUS_RECRUITING.equals(experiment.getStatus())){
            throw new ApiException(400,"当前实验不可报名");
        }

        if(registrationRepository.existsByExperimentIdAndUserId(experimentId,user.getId())){
            throw new ApiException(400,"不能重复报名同一个实验");
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

        return toResponse(registrationRepository.save(registration));
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

        return toResponse(registrationRepository.save(registration));
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

        registration.setStatus(RegistrationConstants.STATUS_CANCELLED);
        registration.setUpdatedAt(LocalDateTime.now());

        return toResponse(registrationRepository.save(registration));
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
