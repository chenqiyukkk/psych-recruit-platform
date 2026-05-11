package com.project.user.controller;

import com.project.common.api.Result;
import com.project.user.dto.AuthLoginRequest;
import com.project.user.dto.AuthLoginResponse;
import com.project.user.dto.AuthRegisterRequest;
import com.project.user.service.UserService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;

  @PostMapping("/register")
  public Result<Void> register(@Valid @RequestBody AuthRegisterRequest request) {
    userService.register(request);
    return Result.success(null);
  }

  @PostMapping("/login")
  public Result<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
    return Result.success(userService.login(request));
  }

  @PostMapping("/logout")
  public Result<Void> logout() {
    return Result.success(null);
  }
}

