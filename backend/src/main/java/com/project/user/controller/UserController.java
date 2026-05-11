package com.project.user.controller;

import com.project.common.api.Result;
import com.project.user.dto.UserProfileResponse;
import com.project.user.dto.UserProfileUpdateRequest;
import com.project.user.service.UserService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/profile")
  public Result<UserProfileResponse> profile(Authentication authentication) {
    String username = authentication.getName();
    return Result.success(userService.getProfile(username));
  }

  @PutMapping("/profile")
  public Result<UserProfileResponse> updateProfile(
      Authentication authentication, @Valid @RequestBody UserProfileUpdateRequest request) {
    String username = authentication.getName();
    return Result.success(userService.updateProfile(username, request));
  }

  @GetMapping("/{id}/rating")
  public Result<Double> researcherRating(@PathVariable("id") Long id) {
    return Result.success(userService.getResearcherRating(id));
  }
}

