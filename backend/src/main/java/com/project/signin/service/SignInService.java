package com.project.signin.service;


import com.project.common.exception.ApiException;
import com.project.experiment.ExperimentConstants;
import com.project.experiment.entity.Experiment;
import com.project.experiment.repo.ExperimentRepository;
import com.project.notification.service.NotificationService;
import com.project.registration.RegistrationConstants;
import com.project.registration.dto.RegistrationResponse;
import com.project.registration.entity.Registration;
import com.project.registration.repo.RegistrationRepository;
import com.project.reputation.ReputationConstants;
import com.project.reputation.entity.ReputationLog;
import com.project.reputation.repo.ReputationRepository;
import com.project.user.UserRoles;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SignInService {
    private final RegistrationRepository registrationRepository;
    private final ExperimentRepository experimentRepository;
    private final UserRepository userRepository;
    private final ReputationRepository reputationRepository;
    private final NotificationService notificationService;

    @Transactional
    public RegistrationResponse signIn(String username, Long registrationId){
        Registration registration = getRegistrationById(registrationId);
        Experiment experiment = getExperimentById(registration.getExperimentId());

        assertCanManageExperiment(username,experiment);

        if(!RegistrationConstants.STATUS_APPROVED.equals(registration.getStatus())){
            throw new ApiException(400,"只有已通过的报名可以签到");
        }

        if(registration.getSignInTime() != null){
            throw new ApiException(400,"该报名已签到");
        }

        LocalDateTime now = LocalDateTime.now();
        registration.setSignInTime(now);
        registration.setUpdatedAt(now);
        registrationRepository.save(registration);

        notificationService.send(registration.getUserId(),
            "已签到", "你已成功签到实验「" + experiment.getTitle() + "」。",
            "SIGN_IN_COMPLETED", "registration", registration.getId());

        return toResponse(registration);
    }

    @Transactional
    public RegistrationResponse complete(String username,Long registrationId){
        Registration registration = getRegistrationById(registrationId);
        Experiment experiment = getExperimentById(registration.getExperimentId());

        assertCanManageExperiment(username,experiment);

        if(registration.getSignInTime() == null){
            throw new ApiException(400,"未签到的报名不能标记完成");
        }

        if(Boolean.TRUE.equals(registration.getIsCompleted())){
            throw new ApiException(400,"该报名已经完成");
        }

        registration.setIsCompleted(true);
        registration.setUpdatedAt(LocalDateTime.now());
        registrationRepository.save(registration);

        // 按时完成，给被试 +2 信誉分
        addReputation(registration.getUserId(), ReputationConstants.CHANGE_TYPE_COMPLETED,
            ReputationConstants.COMPLETED_BONUS, "按时完成实验", registration.getId());

        notificationService.send(registration.getUserId(),
            "实验已完成", "你参加的实验「" + experiment.getTitle() + "」已标记完成，信誉分 +2。",
            "EXPERIMENT_COMPLETED", "registration", registration.getId());

        return toResponse(registration);
    }

    @Transactional
    public RegistrationResponse markNoShow(String username, Long registrationId) {
        Registration registration = getRegistrationById(registrationId);
        Experiment experiment = getExperimentById(registration.getExperimentId());

        assertCanManageExperiment(username, experiment);

        if (!RegistrationConstants.STATUS_APPROVED.equals(registration.getStatus())) {
            throw new ApiException(400, "只有已通过的报名可标记爽约");
        }

        if (registration.getSignInTime() != null) {
            throw new ApiException(400, "已签到的报名不能标记爽约");
        }

        if (Boolean.TRUE.equals(registration.getIsCompleted())) {
            throw new ApiException(400, "已完成的报名不能标记爽约");
        }

        // 爽约：取消报名状态
        registration.setStatus(RegistrationConstants.STATUS_CANCELLED);
        registration.setUpdatedAt(LocalDateTime.now());
        registrationRepository.save(registration);

        // 扣 20 信誉分
        addReputation(registration.getUserId(), ReputationConstants.CHANGE_TYPE_NO_SHOW,
            ReputationConstants.NO_SHOW_PENALTY, "报名通过但未到场（爽约）", registration.getId());

        notificationService.send(registration.getUserId(),
            "爽约记录", "你报名参加的实验「" + experiment.getTitle() + "」已被标记为爽约，信誉分 -20。如有异议可发起申诉。",
            "NO_SHOW_RECORDED", "registration", registration.getId());

        // 爽约后名额释放，若实验满员则恢复为招募中
        if (ExperimentConstants.STATUS_FULL.equals(experiment.getStatus())
            && experiment.getParticipantLimit() != null) {
            long approvedCount = registrationRepository.countByExperimentIdAndStatusIn(
                experiment.getId(), Set.of(RegistrationConstants.STATUS_APPROVED));
            if (approvedCount < experiment.getParticipantLimit()) {
                experiment.setStatus(ExperimentConstants.STATUS_PUBLISHED);
                experiment.setUpdatedAt(LocalDateTime.now());
                experimentRepository.save(experiment);
            }
        }

        return toResponse(registration);
    }

    private void addReputation(Long userId, String changeType, int delta, String reason, Long registrationId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        int newScore = user.getReputationScore() + delta;
        if (newScore < 0) newScore = 0;
        if (newScore > 100) newScore = 100;
        user.setReputationScore(newScore);
        userRepository.save(user);

        ReputationLog log = new ReputationLog();
        log.setUserId(userId);
        log.setRegistrationId(registrationId);
        log.setChangeType(changeType);
        log.setScoreDelta(delta);
        log.setReason(reason);
        log.setCreatedAt(LocalDateTime.now());
        reputationRepository.save(log);
    }

    private Registration getRegistrationById(Long registrationId){
        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ApiException(404,"报名记录不存在"));
    }

    private Experiment getExperimentById(Long experimentId){
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ApiException(404,"实验不存在"));
    }

    private User getUserByUsername(String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(401,"未登录或者用户不存在"));
    }

    private void assertCanManageExperiment(String username, Experiment experiment){
        User operator = getUserByUsername(username);

        if(UserRoles.ADMIN.equals(operator.getRole())){
            return;
        }

        if(!UserRoles.RESEARCHER.equals(operator.getRole())){
            throw new ApiException(403,"无权限操作签到");
        }

        if(!Objects.equals(experiment.getOrganizerId(),operator.getId())){
            throw new ApiException(403,"只能操作自己创建的实验");
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
