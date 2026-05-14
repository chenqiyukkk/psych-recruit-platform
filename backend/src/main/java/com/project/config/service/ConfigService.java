package com.project.config.service;

import com.project.common.exception.ApiException;
import com.project.config.dto.ConfigUpdateRequest;
import com.project.config.entity.Config;
import com.project.config.repository.ConfigRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统配置服务层。
 *
 * <p>查询接口使用 Spring Cache（默认内存缓存）加速，
 * 更新时主动清除对应缓存，保证一致性。
 * 若项目集成 Redis，只需在 application.yml 中配置 spring.cache.type=redis
 * 即可将缓存后端切换为 Redis，业务代码无需修改。
 */
@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final String CACHE_NAME = "sysConfig";

    private final ConfigRepository configRepository;

    // ------------------------------------------------------------------ 查询

    /**
     * 获取所有配置项列表（管理员使用）。
     */
    public List<Config> listAll() {
        return configRepository.findAll();
    }

    /**
     * 根据配置键获取配置值字符串。
     *
     * @param key 配置键
     * @return 配置值
     * @throws ApiException 404 当配置键不存在时
     */
    @Cacheable(cacheNames = CACHE_NAME, key = "#key")
    public String getValue(String key) {
        return configRepository.findByConfigKey(key)
                .map(Config::getConfigValue)
                .orElseThrow(() -> new ApiException(404, "配置项不存在：" + key));
    }

    /**
     * 获取实验类型列表（逗号分隔字符串拆分为 List）。
     */
    public List<String> getExperimentTypes() {
        return splitByComma(getValue("experiment_types"));
    }

    /**
     * 获取常用地点列表。
     */
    public List<String> getLocations() {
        return splitByComma(getValue("locations"));
    }

    /**
     * 获取实验标签列表。
     */
    public List<String> getTags() {
        return splitByComma(getValue("experiment_tags"));
    }

    // ------------------------------------------------------------------ 更新（管理员）

    /**
     * 更新指定配置项的值（管理员专用）。
     * 更新完成后清除该 key 对应的缓存。
     *
     * @param key     配置键
     * @param request 包含新 configValue 和可选 description 的请求体
     * @return 更新后的配置实体
     */
    @Transactional
    @CacheEvict(cacheNames = CACHE_NAME, key = "#key")
    public Config update(String key, ConfigUpdateRequest request) {
        Config config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new ApiException(404, "配置项不存在：" + key));

        config.setConfigValue(request.getConfigValue());
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }
        return configRepository.save(config);
    }

    // ------------------------------------------------------------------ 私有工具

    private List<String> splitByComma(String value) {
        return Arrays.asList(value.split(",\\s*"));
    }
}
