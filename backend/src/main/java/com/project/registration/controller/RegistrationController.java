package com.project.registration.controller;


import com.project.common.api.Result;
import com.project.registration.dto.RegistrationResponse;
import com.project.registration.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/experiments/{experimentId}")
    public Result<RegistrationResponse> apply(
            Authentication authentication,
            @PathVariable("experimentId") Long experimentId){
        return Result.success(registrationService.apply(authentication.getName(),experimentId));
    }

    @GetMapping("/my")
    public Result<List<RegistrationResponse>> getMyRegistration(Authentication authentication){
        return Result.success(registrationService.getMyRegistrations(authentication.getName()));
    }

    @GetMapping("/experiment/{experimentId}")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    public Result<List<RegistrationResponse>> getExperimentRegistration(
            Authentication authentication,
            @PathVariable("experimentId") Long experimentId){
        return Result.success(
                registrationService.getExperimentRegistrations(authentication.getName(),experimentId));
    }

    @PostMapping("{id}/approve")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    public Result<RegistrationResponse> approve(
            Authentication authentication,
            @PathVariable("id") Long id){
        return Result.success(registrationService.approve(authentication.getName(),id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    public Result<RegistrationResponse> reject(
            Authentication authentication,
            @PathVariable("id") Long id){
        return Result.success(registrationService.reject(authentication.getName(),id));
    }

    @PostMapping("{id}/cancel")
    public Result<RegistrationResponse> cancel(
            Authentication authentication,
            @PathVariable("id") Long id){
        return Result.success(registrationService.cancel(authentication.getName(),id));
    }
}
