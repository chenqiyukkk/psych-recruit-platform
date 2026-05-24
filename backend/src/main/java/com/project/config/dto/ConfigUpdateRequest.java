package com.project.config.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新配置项的请求体（管理员专用）。
 */
@Data
public class ConfigUpdateRequest {

    /** 新的配置值 */
    @NotBlank(message = "配置值不能为空")
    private String configValue;

    /** 配置说明（可选，传 null 则不更新） */
    private String description;
}
