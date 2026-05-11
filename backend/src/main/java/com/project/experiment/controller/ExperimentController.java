package com.project.experiment.controller;

import com.project.common.api.Result;
import com.project.experiment.dto.ExperimentCreateRequest;
import com.project.experiment.dto.ExperimentQueryRequest;
import com.project.experiment.dto.ExperimentResponse;
import com.project.experiment.dto.ExperimentUpdateRequest;
import com.project.experiment.service.ExperimentService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments")
@RequiredArgsConstructor
public class ExperimentController {

  private final ExperimentService experimentService;

  @PostMapping
  @PreAuthorize("hasAnyRole('研究者','管理员')")
  public Result<ExperimentResponse> create(
      Authentication authentication, @Valid @RequestBody ExperimentCreateRequest request) {
    return Result.success(experimentService.create(request, authentication.getName()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('研究者','管理员')")
  public Result<ExperimentResponse> update(
      Authentication authentication,
      @PathVariable("id") Long id,
      @RequestBody ExperimentUpdateRequest request) {
    return Result.success(experimentService.update(id, request, authentication.getName()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('研究者','管理员')")
  public Result<Void> delete(Authentication authentication, @PathVariable("id") Long id) {
    experimentService.delete(id, authentication.getName());
    return Result.success(null);
  }

  @GetMapping("/{id}")
  public Result<ExperimentResponse> get(@PathVariable("id") Long id) {
    return Result.success(experimentService.getById(id));
  }

  @GetMapping
  public Result<Page<ExperimentResponse>> query(
      @ModelAttribute ExperimentQueryRequest query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return Result.success(experimentService.query(query, page, size));
  }

  @PostMapping("/{id}/publish")
  @PreAuthorize("hasAnyRole('研究者','管理员')")
  public Result<Void> publish(Authentication authentication, @PathVariable("id") Long id) {
    experimentService.publish(id, authentication.getName());
    return Result.success(null);
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyRole('研究者','管理员')")
  public Result<Void> cancel(Authentication authentication, @PathVariable("id") Long id) {
    experimentService.cancel(id, authentication.getName());
    return Result.success(null);
  }
}

