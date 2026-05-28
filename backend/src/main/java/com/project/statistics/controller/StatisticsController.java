package com.project.statistics.controller;


import com.project.common.api.Result;
import com.project.statistics.dto.ExperimentStatisticsResponse;
import com.project.statistics.dto.PlatformSummaryResponse;
import com.project.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('管理员')")
    public Result<PlatformSummaryResponse> getSummary(){
        return Result.success(statisticsService.getSummary());
    }

    @GetMapping("/experiments/{experimentId}")
    @PreAuthorize("hasAnyRole('管理员','研究者')")
    public  Result<ExperimentStatisticsResponse> getExperimentStatistics(@PathVariable("experimentId") Long experimentId){
        return Result.success(statisticsService.getExperimentStatistics(experimentId));
    }

    @GetMapping("/exports/registrations")
    @PreAuthorize("hasRole('管理员')")
    public ResponseEntity<byte[]> exportRegistrations(){
        byte[] csvBytes = statisticsService.exportRegistrationCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=registration.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csvBytes);
    }
}
