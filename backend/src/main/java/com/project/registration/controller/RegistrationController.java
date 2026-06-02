package com.project.registration.controller;


import com.project.common.api.Result;
import com.project.registration.dto.RegistrationResponse;
import com.project.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/registrations")
@RequiredArgsConstructor
@Tag(name = "报名模块", description = "被试报名实验、研究者审核报名以及用户取消报名")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/experiments/{experimentId}")
    @Operation(summary = "报名实验", description = "当前登录用户作为被试报名指定实验，系统会校验是否已重复报名以及实验是否可报名")
    public Result<RegistrationResponse> apply(
            Authentication authentication,
            @PathVariable("experimentId") Long experimentId){
        return Result.success(registrationService.apply(authentication.getName(),experimentId));
    }

    @GetMapping("/my")
    @Operation(summary = "查询我的报名", description = "查询当前登录用户提交过的所有实验报名记录")
    public Result<List<RegistrationResponse>> getMyRegistration(Authentication authentication){
        return Result.success(registrationService.getMyRegistrations(authentication.getName()));
    }

    @GetMapping("/experiment/{experimentId}")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    @Operation(summary = "查询实验报名列表", description = "研究者或管理员查询指定实验下的报名记录，用于后续审核")
    public Result<List<RegistrationResponse>> getExperimentRegistration(
            Authentication authentication,
            @PathVariable("experimentId") Long experimentId){
        return Result.success(
                registrationService.getExperimentRegistrations(authentication.getName(),experimentId));
    }

    @PostMapping("{id}/approve")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    @Operation(summary = "通过报名申请", description = "研究者或管理员将指定报名记录审核为通过状态")
    public Result<RegistrationResponse> approve(
            Authentication authentication,
            @PathVariable("id") Long id){
        return Result.success(registrationService.approve(authentication.getName(),id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    @Operation(summary = "拒绝报名申请", description = "研究者或管理员将指定报名记录审核为拒绝状态")
    public Result<RegistrationResponse> reject(
            Authentication authentication,
            @PathVariable("id") Long id){
        return Result.success(registrationService.reject(authentication.getName(),id));
    }

    @PostMapping("{id}/cancel")
    @Operation(summary = "取消报名", description = "当前登录用户取消自己的报名记录")
    public Result<RegistrationResponse> cancel(
            Authentication authentication,
            @PathVariable("id") Long id){
        return Result.success(registrationService.cancel(authentication.getName(),id));
    }
}
