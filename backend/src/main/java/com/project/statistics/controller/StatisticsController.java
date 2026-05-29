package com.project.statistics.controller;


import com.project.common.api.Result;
import com.project.statistics.dto.ExperimentStatisticsResponse;
import com.project.statistics.dto.PlatformSummaryResponse;
import com.project.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "统计导出模块", description = "平台总览统计、单个实验统计和报名数据 CSV 导出")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('管理员')")
    @Operation(summary = "查询平台统计总览", description = "管理员查询平台用户数、实验数、报名数和评价数等总体数据")
    public Result<PlatformSummaryResponse> getSummary(){
        return Result.success(statisticsService.getSummary());
    }

    @GetMapping("/experiments/{experimentId}")
    @PreAuthorize("hasAnyRole('管理员','研究者')")
    @Operation(summary = "查询单个实验统计", description = "管理员或研究者查询指定实验的报名、审核、签到和完成情况")
    public  Result<ExperimentStatisticsResponse> getExperimentStatistics(@PathVariable("experimentId") Long experimentId){
        return Result.success(statisticsService.getExperimentStatistics(experimentId));
    }

    @GetMapping("/exports/registrations")
    @PreAuthorize("hasRole('管理员')")
    @Operation(summary = "导出报名数据 CSV", description = "管理员导出全平台报名记录，返回 CSV 文件内容")
    public ResponseEntity<byte[]> exportRegistrations(){
        byte[] csvBytes = statisticsService.exportRegistrationCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=registration.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csvBytes);
    }
}
