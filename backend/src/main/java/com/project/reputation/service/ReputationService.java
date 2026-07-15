package com.project.reputation.service;


import com.project.common.exception.ApiException;
import com.project.reputation.ReputationConstants;
import com.project.reputation.dto.ReputationAdjustRequest;
import com.project.reputation.dto.ReputationLogResponse;
import com.project.reputation.dto.ReputationResponse;
import com.project.reputation.entity.ReputationLog;
import com.project.reputation.repo.ReputationRepository;
import com.project.user.UserRoles;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReputationService {

    private final UserRepository userRepository;

    private  final ReputationRepository reputationRepository;

    public ReputationResponse getMyReputation(String username){
        User user = getUserByUsername(username);
        return toReputationResponse(user);
    }

    public List<ReputationLogResponse> getMyLogs(String username){
        User user = getUserByUsername(username);
        return getUserLogs(user.getId());
    }

    public ReputationResponse getUserReputation(Long userId){
        User user = getUserById(userId);
        return toReputationResponse(user);
    }

    public List<ReputationLogResponse> getUserLogs(Long userId){
        getUserById(userId);

        return reputationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toLogResponse)
                .collect(Collectors.toList());
    }


    @Transactional
    public ReputationResponse adjust(String operatorUsername, Long userId, ReputationAdjustRequest request){

        User operator = getUserByUsername(operatorUsername);

        if(!UserRoles.ADMIN.equals(operator.getRole())){
            throw new ApiException(403,"只有管理员可以调整信誉分");
        }

        User target = getUserById(userId);

        int oldScore = target.getReputationScore();
        int newScore = oldScore + request.getScoreDelta();

        if(newScore < ReputationConstants.MIN_SCORE || newScore > ReputationConstants.MAX_SCORE){
            throw new ApiException(400,"调整后的信誉分必须在0到100之间");
        }

        target.setReputationScore(newScore);
        userRepository.save(target);

        ReputationLog log = new ReputationLog();
        log.setUserId(userId);
        log.setChangeType(ReputationConstants.CHANGE_TYPE_ADMIN_ADJUST);
        log.setScoreDelta(request.getScoreDelta());
        log.setReason(request.getReason());
        log.setCreatedAt(LocalDateTime.now());
        reputationRepository.save(log);

        return toReputationResponse(target);
    }

    private User getUserByUsername(String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(401,"未登录或用户不存在"));
    }

    private User getUserById(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404,"用户不存在"));
    }

    private ReputationResponse toReputationResponse(User user){
        return new ReputationResponse(
                user.getId(),
                user.getUsername(),
                user.getReputationScore()
        );
    }

    private ReputationLogResponse toLogResponse(ReputationLog log){
        return new ReputationLogResponse(
                log.getId(),
                log.getUserId(),
                log.getChangeType(),
                log.getScoreDelta(),
                log.getReason(),
                log.getCreatedAt()
        );
    }
}
