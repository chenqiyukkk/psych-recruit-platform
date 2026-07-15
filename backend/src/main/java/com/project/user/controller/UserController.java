package com.project.user.controller;

import com.project.common.api.Result;
import com.project.user.dto.UserListResponse;
import com.project.user.dto.UserProfileResponse;
import com.project.user.dto.UserProfileUpdateRequest;
import com.project.user.service.UserService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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

  @GetMapping
  @PreAuthorize("hasRole('管理员')")
  public Result<List<UserListResponse>> listAll() {
    return Result.success(userService.listAll());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('管理员')")
  public Result<Void> delete(@PathVariable("id") Long id) {
    userService.deleteUser(id);
    return Result.success(null);
  }

  @PutMapping("/{id}/reset-password")
  @PreAuthorize("hasRole('管理员')")
  public Result<Void> resetPassword(@PathVariable("id") Long id,
      @RequestBody java.util.Map<String, String> body) {
    userService.resetPassword(id, body.get("password"));
    return Result.success(null);
  }

  @PutMapping("/{id}/enable")
  @PreAuthorize("hasRole('管理员')")
  public Result<Void> enable(@PathVariable("id") Long id,
      @RequestBody(required = false) java.util.Map<String, String> body) {
    String role = body != null ? body.get("role") : null;
    userService.enableUser(id, role);
    return Result.success(null);
  }
}

