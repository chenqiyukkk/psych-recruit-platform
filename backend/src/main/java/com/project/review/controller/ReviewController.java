package com.project.review.controller;

import com.project.common.api.Result;
import com.project.review.dto.ReviewCreateRequest;
import com.project.review.dto.ReviewResponse;
import com.project.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/registrations/{registrationId}")
    public Result<ReviewResponse> create(
            Authentication authentication,
            @PathVariable("registrationId") Long registrationId,
            @Valid @RequestBody ReviewCreateRequest request){
        return Result.success(reviewService.create(authentication.getName(),registrationId,request));
    }

    @GetMapping("/my")
    public Result<List<ReviewResponse>> getMyReviews(Authentication authentication){
        return Result.success(reviewService.getMyReviews(authentication.getName()));
    }

    @GetMapping("/received")
    public Result<List<ReviewResponse>> getReceivedReviews(Authentication authentication){
        return Result.success(reviewService.getReceivedReviews(authentication.getName()));
    }

}
