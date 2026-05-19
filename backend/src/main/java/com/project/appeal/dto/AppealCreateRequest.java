package com.project.appeal.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 提交申诉请求体。
 */
@Getter
@Setter
public class AppealCreateRequest {

    /**
     * 申诉类型，必填。
     * 枚举值：REPUTATION_DEDUCTION / LOW_RATING / PAYMENT_DISPUTE
     */
    @NotBlank(message = "申诉类型不能为空")
    @Pattern(
        regexp = "REPUTATION_DEDUCTION|LOW_RATING|PAYMENT_DISPUTE",
        message = "申诉类型必须为 REPUTATION_DEDUCTION、LOW_RATING 或 PAYMENT_DISPUTE"
    )
    private String appealType;

    /**
     * 关联记录ID，必填。
     * 根据 appealType 对应不同表的主键：
     *   REPUTATION_DEDUCTION → reputation_logs.id
     *   LOW_RATING           → reviews.id
     *   PAYMENT_DISPUTE      → payment_records.id
     */
    @NotNull(message = "关联记录ID不能为空")
    private Long targetId;

    /** 申诉理由，必填，至少10字 */
    @NotBlank(message = "申诉理由不能为空")
    private String reason;

    /**
     * 证据图片URL列表，JSON格式，可为空。
     * 示例：["https://cdn.example.com/img1.jpg"]
     */
    private String evidenceUrls;
}
