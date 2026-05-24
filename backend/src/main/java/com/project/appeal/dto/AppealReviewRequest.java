package com.project.appeal.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 管理员审核申诉请求体。
 */
@Getter
@Setter
public class AppealReviewRequest {

    /**
     * 审核结论，必填。
     * 枚举值：APPROVED（通过）/ REJECTED（拒绝）
     */
    @NotBlank(message = "审核结论不能为空")
    @Pattern(regexp = "APPROVED|REJECTED", message = "审核结论必须为 APPROVED 或 REJECTED")
    private String decision;

    /** 审核意见，可为空 */
    private String reviewComment;
}
