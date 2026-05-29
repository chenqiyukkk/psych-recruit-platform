package com.project.statistics.service;


import com.project.common.exception.ApiException;
import com.project.experiment.entity.Experiment;
import com.project.experiment.repo.ExperimentRepository;
import com.project.registration.RegistrationConstants;
import com.project.registration.entity.Registration;
import com.project.registration.repo.RegistrationRepository;
import com.project.review.repo.ReviewRepository;
import com.project.statistics.dto.ExperimentStatisticsResponse;
import com.project.statistics.dto.PlatformSummaryResponse;
import com.project.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final ExperimentRepository experimentRepository;
    private final RegistrationRepository registrationRepository;
    private final ReviewRepository reviewRepository;

    public PlatformSummaryResponse getSummary(){
        return new PlatformSummaryResponse(
                userRepository.count(),
                experimentRepository.count(),
                registrationRepository.count(),
                reviewRepository.count()
        );
    }

    public ExperimentStatisticsResponse getExperimentStatistics(Long experimentId){
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ApiException(404,"实验不存在"));

        List<Registration> registrations =
                registrationRepository.findByExperimentIdOrderByAppliedAtDesc(experimentId);

        long registrationCount = registrations.size();

        long approvedCount = registrations.stream()
                .filter(r -> RegistrationConstants.STATUS_APPROVED.equals(r.getStatus()))
                .count();

        long rejectedCount = registrations.stream()
                .filter(r -> RegistrationConstants.STATUS_REJECTED.equals(r.getStatus()))
                .count();

        long cancelledCount = registrations.stream()
                .filter(r -> RegistrationConstants.STATUS_CANCELLED.equals(r.getStatus()))
                .count();

        long signedInCount = registrations.stream()
                .filter(r -> r.getSignInTime() != null)
                .count();

        long completedCount = registrations.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCompleted()))
                .count();
        return new ExperimentStatisticsResponse(
                experiment.getId(),
                experiment.getTitle(),
                registrationCount,
                approvedCount,
                rejectedCount,
                cancelledCount,
                signedInCount,
                completedCount
        );
    }

    public byte[] exportRegistrationCsv(){
        List<Registration> registrations = registrationRepository.findAll();
        String header = "id,experimentId,userId,status,appliedAt,reviewedAt,signInTime,isCompleted,createdAt,updatedAt\n";

        String body = registrations.stream()
                .map(this::toRegistrationCsvLine)
                .collect(Collectors.joining("\n"));

        String csv = header +body;
        return csv.getBytes(StandardCharsets.UTF_8);
    }


    private String toRegistrationCsvLine(Registration r){
        return String.join(",",
                String.valueOf(r.getId()),
                String.valueOf(r.getExperimentId()),
                String.valueOf(r.getUserId()),
                nullToEmpty(r.getStatus()),
                nullToEmpty(r.getAppliedAt()),
                nullToEmpty(r.getReviewedAt()),
                nullToEmpty(r.getSignInTime()),
                String.valueOf(Boolean.TRUE.equals(r.getIsCompleted())),
                nullToEmpty(r.getCreatedAt()),
                nullToEmpty(r.getUpdatedAt()));
    }
    private String nullToEmpty(Object value){
        return value == null ? "" :String.valueOf(value);
    }
}
