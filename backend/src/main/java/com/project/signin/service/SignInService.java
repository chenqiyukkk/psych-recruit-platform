package com.project.signin.service;


import com.project.common.exception.ApiException;
import com.project.experiment.entity.Experiment;
import com.project.experiment.repo.ExperimentRepository;
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
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SignInService {
    private final RegistrationRepository registrationRepository;
    private final ExperimentRepository experimentRepository;
    private final UserRepository userRepository;

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

        return toResponse(registrationRepository.save(registration));
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

        return toResponse(registrationRepository.save(registration));
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
