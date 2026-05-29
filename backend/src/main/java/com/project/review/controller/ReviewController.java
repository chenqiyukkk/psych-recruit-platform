package com.project.review.controller;

import com.project.common.api.Result;
import com.project.review.dto.ReviewCreateRequest;
import com.project.review.dto.ReviewResponse;
import com.project.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "评价模块", description = "实验完成后的双向评价、我发出的评价和我收到的评价查询")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/registrations/{registrationId}")
    @Operation(summary = "创建评价", description = "当前登录用户基于已完成的报名记录创建评价，支持被试评价研究者和研究者评价被试")
    public Result<ReviewResponse> create(
            Authentication authentication,
            @PathVariable("registrationId") Long registrationId,
            @Valid @RequestBody ReviewCreateRequest request){
        return Result.success(reviewService.create(authentication.getName(),registrationId,request));
    }

    @GetMapping("/my")
    @Operation(summary = "查询我发出的评价", description = "查询当前登录用户作为评价人提交过的评价记录")
    public Result<List<ReviewResponse>> getMyReviews(Authentication authentication){
        return Result.success(reviewService.getMyReviews(authentication.getName()));
    }

    @GetMapping("/received")
    @Operation(summary = "查询我收到的评价", description = "查询当前登录用户作为被评价人收到的评价记录")
    public Result<List<ReviewResponse>> getReceivedReviews(Authentication authentication){
        return Result.success(reviewService.getReceivedReviews(authentication.getName()));
    }

}
