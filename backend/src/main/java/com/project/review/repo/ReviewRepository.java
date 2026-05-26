package com.project.review.repo;

import com.project.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review,Long> {

    Boolean existsByRegistrationIdAndReviewType(Long registrationId,String reviewType);

    List<Review> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId);

    List<Review> findByReviewedIdOrderByCreatedAtDesc(Long reviewedId);
}
