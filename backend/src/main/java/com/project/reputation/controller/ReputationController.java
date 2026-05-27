package com.project.reputation.controller;


import com.project.common.api.Result;
import com.project.reputation.dto.ReputationAdjustRequest;
import com.project.reputation.dto.ReputationLogResponse;
import com.project.reputation.dto.ReputationResponse;
import com.project.reputation.service.ReputationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/reputations")
@RequiredArgsConstructor
public class ReputationController {

    private final ReputationService reputationService;

    @GetMapping("/my")
    public Result<ReputationResponse> getMyReputation(Authentication authentication){
        return Result.success(reputationService.getMyReputation(authentication.getName()));
    }

    @GetMapping("/users/{userId}")
    public Result<ReputationResponse> getUserReputation(@PathVariable("userId") Long userId){
        return Result.success(reputationService.getUserReputation(userId));
    }

    @GetMapping("/users/{userId}/logs")
    public Result<List<ReputationLogResponse>> getUserLogs(@PathVariable("userId") Long userId){
        return Result.success(reputationService.getUserLogs(userId));
    }

    @PostMapping("/users/{userId}/adjust")
    @PreAuthorize("hasRole('管理员')")
    public Result<ReputationResponse> adjust(
            Authentication authentication,
            @PathVariable("userId") Long userId,
            @Valid @RequestBody ReputationAdjustRequest request){
        return Result.success(reputationService.adjust(authentication.getName(),userId,request));
    }
}
