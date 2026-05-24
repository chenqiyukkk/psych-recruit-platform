package com.project.config.controller;

import com.project.common.api.Result;
import com.project.config.dto.ConfigUpdateRequest;
import com.project.config.entity.Config;
import com.project.config.service.ConfigService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统配置控制器。
 *
 * <p>公开只读接口（实验类型、地点、标签）供前端下拉框使用；
 * 写接口需要 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    // -------- 公开只读接口 --------

    /**
     * GET /api/config/experiment-types
     * 获取实验类型列表。
     */
    @GetMapping("/experiment-types")
    public Result<List<String>> getExperimentTypes() {
        return Result.success(configService.getExperimentTypes());
    }

    /**
     * GET /api/config/locations
     * 获取常用地点列表。
     */
    @GetMapping("/locations")
    public Result<List<String>> getLocations() {
        return Result.success(configService.getLocations());
    }

    /**
     * GET /api/config/tags
     * 获取实验标签列表。
     */
    @GetMapping("/tags")
    public Result<List<String>> getTags() {
        return Result.success(configService.getTags());
    }

    // -------- 管理员接口 --------

    /**
     * GET /api/config
     * 获取所有配置项（管理员）。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Config>> listAll() {
        return Result.success(configService.listAll());
    }

    /**
     * PUT /api/config/{key}
     * 更新指定配置项（管理员）。
     *
     * @param key     配置键，例如 experiment_types
     * @param request 包含新值和可选说明
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Config> update(
            @PathVariable String key,
            @Valid @RequestBody ConfigUpdateRequest request) {
        return Result.success(configService.update(key, request));
    }
}
