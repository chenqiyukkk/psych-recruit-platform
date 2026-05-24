package com.project.config.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统配置项实体类，对应 sys_config 表。
 *
 * <p>每一行代表一个键值对配置，例如：
 * <pre>
 *   config_key   = "experiment_types"
 *   config_value = "认知类,情绪类,社会类,临床类,发展类"
 *   description  = "实验类型枚举值，用逗号分隔"
 * </pre>
 */
@Getter
@Setter
@Entity
@Table(name = "sys_config")
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配置键（全局唯一） */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /** 配置值（字符串，复杂结构存 JSON） */
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    /** 配置说明 */
    @Column(length = 255)
    private String description;
}
