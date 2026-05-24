package com.project.signin.controller;


import com.project.common.api.Result;
import com.project.registration.dto.RegistrationResponse;
import com.project.signin.service.SignInService;
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
public class SignInController {

    private final SignInService signInService;

    @PostMapping("/registrations/{registrationId}")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    public Result<RegistrationResponse> signIn(
            Authentication authentication,
            @PathVariable("registrationId") Long registrationId){
        return Result.success(signInService.signIn(authentication.getName(),registrationId));
    }

    @PostMapping("/registrations/{registrationId}/complete")
    @PreAuthorize("hasAnyRole('研究者','管理员')")
    public Result<RegistrationResponse> complete(
            Authentication authentication,
            @PathVariable("registrationId") Long registrationId){
        return Result.success(signInService.complete(authentication.getName(),registrationId));
    }

}
