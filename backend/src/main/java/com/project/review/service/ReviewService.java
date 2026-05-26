package com.project.review.service;


import com.project.common.exception.ApiException;
import com.project.experiment.entity.Experiment;
import com.project.experiment.repo.ExperimentRepository;
import com.project.registration.RegistrationConstants;
import com.project.registration.entity.Registration;
import com.project.registration.repo.RegistrationRepository;
import com.project.review.ReviewConstants;
import com.project.review.dto.ReviewCreateRequest;
import com.project.review.dto.ReviewResponse;
import com.project.review.entity.Review;
import com.project.review.repo.ReviewRepository;
import com.project.user.UserRoles;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    private final RegistrationRepository registrationRepository;

    private final ExperimentRepository experimentRepository;

    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse create(String username, Long registrationId, ReviewCreateRequest request){

        User reviewer = getUserByUsername(username);
        Registration registration = getRegistrationById(registrationId);
        Experiment experiment = getExperimentById(registration.getExperimentId());

        validateReviewType(request.getReviewType());
        validateRegistrationCanBeReview(registration);

        if(reviewRepository.existsByRegistrationIdAndReviewType(registrationId,request.getReviewType())){
            throw new ApiException(400,"该方向已评价过");
        }

        Long reviewedId = resolveReviewedId(reviewer,registration,experiment,request.getReviewType());

        Review review = new Review();
        review.setRegistrationId(registrationId);
        review.setReviewerId(reviewer.getId());
        review.setReviewedId(reviewedId);
        review.setReviewType(request.getReviewType());
        review.setRating(request.getRating());
        review.setCommunicationScore(request.getCommunicationScore());
        review.setProfessionalismScore(request.getProfessionalismScore());
        review.setPunctualityScore(request.getPunctualityScore());
        review.setComment(request.getComment());
        review.setIsAnonymous(Boolean.TRUE.equals(request.getIsAnonymous()));
        review.setCreatedAt(LocalDateTime.now());

        return toResponse(reviewRepository.save(review));

    }
    public List<ReviewResponse> getMyReviews(String username){

        User user = getUserByUsername(username);

        return reviewRepository.findByReviewerIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getReceivedReviews(String username){

        User user = getUserByUsername(username);

        return reviewRepository.findByReviewedIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void validateReviewType(String reviewType){
        if(!ReviewConstants.REVIEW_TYPES.contains(reviewType)){
            throw new ApiException(400,"评价方向不合法");
        }
    }

    private void validateRegistrationCanBeReview(Registration registration){
        if(!RegistrationConstants.STATUS_APPROVED.equals(registration.getStatus())){
            throw new ApiException(400,"只有已通过的报名可以评价");
        }

        if(registration.getSignInTime() == null){
            throw new ApiException(400,"未签到的报名不能评价");
        }

        if(!Boolean.TRUE.equals(registration.getIsCompleted())){
            throw  new ApiException(400,"未完成的报名不能评价");
        }
    }

    private Long resolveReviewedId(
            User reviewer,
            Registration registration,
            Experiment experiment,
            String reviewType){
        if(ReviewConstants.SUBJECT_TO_RESEARCHER.equals(reviewType)){
            return resolveResearcherIdForSubjectReview(reviewer,registration,experiment);
        }
        if(ReviewConstants.RESEARCHER_TO_SUBJECT.equals(reviewType)){
            return resolveSubjectIdForResearcherReview(reviewer,registration,experiment);
        }
        throw new ApiException(400,"评价方向不合法");
    }

    private Long resolveResearcherIdForSubjectReview(
            User reviewer,
            Registration registration,
            Experiment experiment){
        if(!UserRoles.SUBJECT.equals(reviewer.getRole())){
            throw new ApiException(403,"只有被试可以评价研究者");
        }
        if(!Objects.equals(registration.getUserId(),reviewer.getId())){
            throw new ApiException(403,"只能评价自己参加的实验");
        }
        return experiment.getOrganizerId();
    }

    private  Long resolveSubjectIdForResearcherReview(
            User reviewer,
            Registration registration,
            Experiment experiment){
        if(!UserRoles.RESEARCHER.equals(reviewer.getRole())){
            throw new ApiException(403,"只有研究者或者管理员可以评价被试");
        }
        if(UserRoles.RESEARCHER.equals(reviewer.getRole()) && !Objects.equals(experiment.getOrganizerId(),reviewer.getId())){
            throw new ApiException(403,"只能评价自己实验下的被试");
        }
        return registration.getUserId();
    }

    private User getUserByUsername(String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(403,"未登录或用户不存在"));
    }

    private Registration getRegistrationById(Long registrationId){
        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ApiException(404,"报名记录不存在"));
    }

    private  Experiment getExperimentById(Long experimentId){
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ApiException(404,"实验不存在"));
    }

    private ReviewResponse toResponse(Review review){
        return new ReviewResponse(
                review.getId(),
                review.getRegistrationId(),
                review.getReviewerId(),
                review.getReviewedId(),
                review.getReviewType(),
                review.getRating(),
                review.getCommunicationScore(),
                review.getProfessionalismScore(),
                review.getPunctualityScore(),
                review.getComment(),
                review.getIsAnonymous(),
                review.getCreatedAt()
        );
    }



}
