package com.project.signin.controller;


import com.project.common.api.Result;
import com.project.registration.dto.RegistrationResponse;
import com.project.signin.service.SignInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sign-ins")
@RequiredArgsConstructor
@Tag(name = "签到模块", description = "实验现场签到和实验完成确认")
public class SignInController {

    private final SignInService signInService;

    @PostMapping("/registrations/{registrationId}")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    @Operation(summary = "报名签到", description = "研究者或管理员为已通过审核的报名记录进行现场签到")
    public Result<RegistrationResponse> signIn(
            Authentication authentication,
            @PathVariable("registrationId") Long registrationId){
        return Result.success(signInService.signIn(authentication.getName(),registrationId));
    }

    @PostMapping("/registrations/{registrationId}/complete")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    @Operation(summary = "确认实验完成", description = "研究者或管理员将已签到的报名记录标记为实验完成")
    public Result<RegistrationResponse> complete(
            Authentication authentication,
            @PathVariable("registrationId") Long registrationId){
        return Result.success(signInService.complete(authentication.getName(),registrationId));
    }

    @PostMapping("/registrations/{registrationId}/no-show")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    @Operation(summary = "标记爽约", description = "研究者或管理员将已通过但未签到的报名标记为爽约，被试扣 20 信誉分")
    public Result<RegistrationResponse> markNoShow(
            Authentication authentication,
            @PathVariable("registrationId") Long registrationId){
        return Result.success(signInService.markNoShow(authentication.getName(),registrationId));
    }

}
