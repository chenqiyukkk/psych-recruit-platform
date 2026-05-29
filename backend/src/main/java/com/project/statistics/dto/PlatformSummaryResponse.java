package com.project.statistics.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlatformSummaryResponse {

    private long userCount;

    private long experimentCount;

    private long registrationCount;

    private long reviewCount;
}
