package com.project.statistics.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExperimentStatisticsResponse {

    private Long experimentId;

    private String experimentTitle;

    private long registrationCount;

    private long approvedCount;

    private long rejectedCount;

    private long cancelledCount;

    private long signedInCount;

    private long completedCount;
}
