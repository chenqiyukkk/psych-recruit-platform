package com.project.appeal.controller;

import com.project.appeal.dto.AppealCreateRequest;
import com.project.appeal.dto.AppealResponse;
import com.project.appeal.dto.AppealReviewRequest;
import com.project.appeal.service.AppealService;
import com.project.common.api.Result;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 申诉模块接口。
 *
 * <ul>
 *   <li>POST  /api/appeals            — 提交申诉（登录用户）</li>
 *   <li>GET   /api/appeals/my         — 查询我的申诉列表（登录用户）</li>
 *   <li>GET   /api/appeals            — 分页查询所有申诉（管理员）</li>
 *   <li>PUT   /api/appeals/{id}/review — 审核申诉（管理员）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;
    private final UserRepository userRepository;

    /**
     * 提交申诉。
     *
     * @param request        申诉请求体（appealType、targetId、reason、evidenceUrls）
     * @param authentication Spring Security 认证信息
     * @return 创建后的申诉记录
     */
    @PostMapping
    public Result<AppealResponse> create(
            @Valid @RequestBody AppealCreateRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(appealService.create(userId, request));
    }

    /**
     * 查询当前用户提交的所有申诉（按创建时间倒序）。
     *
     * @param authentication Spring Security 认证信息
     * @return 申诉列表
     */
    @GetMapping("/my")
    public Result<List<AppealResponse>> getMyAppeals(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(appealService.getMyAppeals(userId));
    }

    /**
     * 管理员分页查询所有申诉，支持按状态过滤。
     *
     * @param status 状态过滤，可选：PENDING / UNDER_REVIEW / APPROVED / REJECTED
     * @param page   页码，从 0 开始，默认 0
     * @param size   每页条数，默认 20
     * @return 分页申诉列表
     */
    @GetMapping
    public Result<Page<AppealResponse>> getAllAppeals(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(appealService.getAllAppeals(status, page, size));
    }

    /**
     * 管理员审核申诉（通过或拒绝）。
     *
     * @param id             申诉记录ID
     * @param request        审核请求体（decision: APPROVED/REJECTED，reviewComment）
     * @param authentication Spring Security 认证信息
     * @return 更新后的申诉记录
     */
    @PutMapping("/{id}/review")
    public Result<AppealResponse> review(
            @PathVariable("id") Long id,
            @Valid @RequestBody AppealReviewRequest request,
            Authentication authentication) {
        Long reviewerId = getCurrentUserId(authentication);
        return Result.success(appealService.review(id, reviewerId, request));
    }

    // ==================== 私有方法 ====================

    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return user.getId();
    }
}
