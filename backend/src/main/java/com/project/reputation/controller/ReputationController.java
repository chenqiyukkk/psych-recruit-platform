package com.project.reputation.controller;


import com.project.common.api.Result;
import com.project.reputation.dto.ReputationAdjustRequest;
import com.project.reputation.dto.ReputationLogResponse;
import com.project.reputation.dto.ReputationResponse;
import com.project.reputation.service.ReputationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/reputations")
@RequiredArgsConstructor
@Tag(name = "信誉分模块", description = "用户信誉分查询、信誉分变动记录查询和管理员手动调整")
public class ReputationController {

    private final ReputationService reputationService;

    @GetMapping("/my")
    @Operation(summary = "查询我的信誉分", description = "查询当前登录用户的信誉分、评价次数等信誉信息")
    public Result<ReputationResponse> getMyReputation(Authentication authentication){
        return Result.success(reputationService.getMyReputation(authentication.getName()));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "查询指定用户信誉分", description = "根据用户 ID 查询该用户当前信誉分信息")
    public Result<ReputationResponse> getUserReputation(@PathVariable("userId") Long userId){
        return Result.success(reputationService.getUserReputation(userId));
    }

    @GetMapping("/users/{userId}/logs")
    @Operation(summary = "查询指定用户信誉分记录", description = "根据用户 ID 查询该用户的信誉分变动日志")
    public Result<List<ReputationLogResponse>> getUserLogs(@PathVariable("userId") Long userId){
        return Result.success(reputationService.getUserLogs(userId));
    }

    @PostMapping("/users/{userId}/adjust")
    @PreAuthorize("hasRole('管理员')")
    @Operation(summary = "管理员调整信誉分", description = "管理员手动增加或扣减指定用户的信誉分，并记录调整原因")
    public Result<ReputationResponse> adjust(
            Authentication authentication,
            @PathVariable("userId") Long userId,
            @Valid @RequestBody ReputationAdjustRequest request){
        return Result.success(reputationService.adjust(authentication.getName(),userId,request));
    }
}
