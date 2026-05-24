package com.project.config;

import com.project.common.exception.ApiException;
import com.project.config.dto.ConfigUpdateRequest;
import com.project.config.entity.Config;
import com.project.config.repository.ConfigRepository;
import com.project.config.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ConfigService 单元测试。
 * 使用 Mockito 模拟 ConfigRepository，不依赖数据库。
 */
@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private ConfigRepository configRepository;

    @InjectMocks
    private ConfigService configService;

    // ---- 测试数据 ----

    private Config buildConfig(String key, String value) {
        Config c = new Config();
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setDescription("desc-" + key);
        return c;
    }

    // ================================================================
    // listAll
    // ================================================================

    @Test
    @DisplayName("listAll - 返回所有配置项")
    void listAll_returnsAllConfigs() {
        List<Config> data = Arrays.asList(
                buildConfig("experiment_types", "认知类,情绪类"),
                buildConfig("locations", "逸夫楼,图书馆")
        );
        when(configRepository.findAll()).thenReturn(data);

        List<Config> result = configService.listAll();

        assertThat(result).hasSize(2);
        verify(configRepository).findAll();
    }

    // ================================================================
    // getValue
    // ================================================================

    @Test
    @DisplayName("getValue - key 存在时返回配置值")
    void getValue_existingKey_returnsValue() {
        when(configRepository.findByConfigKey("experiment_types"))
                .thenReturn(Optional.of(buildConfig("experiment_types", "认知类,情绪类")));

        String val = configService.getValue("experiment_types");

        assertThat(val).isEqualTo("认知类,情绪类");
    }

    @Test
    @DisplayName("getValue - key 不存在时抛出 ApiException(404)")
    void getValue_missingKey_throws404() {
        when(configRepository.findByConfigKey("no_such_key"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.getValue("no_such_key"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no_such_key");
    }

    // ================================================================
    // getExperimentTypes
    // ================================================================

    @Test
    @DisplayName("getExperimentTypes - 正确拆分逗号分隔值")
    void getExperimentTypes_splitsCorrectly() {
        when(configRepository.findByConfigKey("experiment_types"))
                .thenReturn(Optional.of(buildConfig("experiment_types", "认知类,情绪类,社会类")));

        List<String> types = configService.getExperimentTypes();

        assertThat(types).containsExactly("认知类", "情绪类", "社会类");
    }

    @Test
    @DisplayName("getExperimentTypes - 值含空格时也能正确拆分")
    void getExperimentTypes_splitsWithSpaces() {
        when(configRepository.findByConfigKey("experiment_types"))
                .thenReturn(Optional.of(buildConfig("experiment_types", "认知类, 情绪类, 发展类")));

        List<String> types = configService.getExperimentTypes();

        assertThat(types).containsExactly("认知类", "情绪类", "发展类");
    }

    // ================================================================
    // getLocations
    // ================================================================

    @Test
    @DisplayName("getLocations - 正确拆分地点列表")
    void getLocations_splitsCorrectly() {
        when(configRepository.findByConfigKey("locations"))
                .thenReturn(Optional.of(buildConfig("locations", "逸夫楼,图书馆,心理实验楼")));

        List<String> locs = configService.getLocations();

        assertThat(locs).containsExactly("逸夫楼", "图书馆", "心理实验楼");
    }

    // ================================================================
    // getTags
    // ================================================================

    @Test
    @DisplayName("getTags - 正确拆分标签列表")
    void getTags_splitsCorrectly() {
        when(configRepository.findByConfigKey("experiment_tags"))
                .thenReturn(Optional.of(buildConfig("experiment_tags", "fMRI,情绪类,认知类")));

        List<String> tags = configService.getTags();

        assertThat(tags).containsExactly("fMRI", "情绪类", "认知类");
    }

    // ================================================================
    // update
    // ================================================================

    @Test
    @DisplayName("update - 成功更新配置值")
    void update_success_updatesValue() {
        Config existing = buildConfig("experiment_types", "认知类,情绪类");
        when(configRepository.findByConfigKey("experiment_types"))
                .thenReturn(Optional.of(existing));
        when(configRepository.save(any(Config.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfigUpdateRequest req = new ConfigUpdateRequest();
        req.setConfigValue("认知类,情绪类,社会类");

        Config updated = configService.update("experiment_types", req);

        assertThat(updated.getConfigValue()).isEqualTo("认知类,情绪类,社会类");
        verify(configRepository).save(existing);
    }

    @Test
    @DisplayName("update - 同时更新 description")
    void update_withDescription_updatesDescription() {
        Config existing = buildConfig("locations", "逸夫楼");
        when(configRepository.findByConfigKey("locations"))
                .thenReturn(Optional.of(existing));
        when(configRepository.save(any(Config.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfigUpdateRequest req = new ConfigUpdateRequest();
        req.setConfigValue("逸夫楼,图书馆");
        req.setDescription("新说明");

        Config updated = configService.update("locations", req);

        assertThat(updated.getConfigValue()).isEqualTo("逸夫楼,图书馆");
        assertThat(updated.getDescription()).isEqualTo("新说明");
    }

    @Test
    @DisplayName("update - description 为 null 时不覆盖原有说明")
    void update_nullDescription_keepsOriginal() {
        Config existing = buildConfig("locations", "逸夫楼");
        existing.setDescription("原说明");
        when(configRepository.findByConfigKey("locations"))
                .thenReturn(Optional.of(existing));
        when(configRepository.save(any(Config.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfigUpdateRequest req = new ConfigUpdateRequest();
        req.setConfigValue("图书馆");
        // description 不设置（null）

        Config updated = configService.update("locations", req);

        assertThat(updated.getDescription()).isEqualTo("原说明");
    }

    @Test
    @DisplayName("update - key 不存在时抛出 ApiException(404)")
    void update_missingKey_throws404() {
        when(configRepository.findByConfigKey("ghost_key"))
                .thenReturn(Optional.empty());

        ConfigUpdateRequest req = new ConfigUpdateRequest();
        req.setConfigValue("value");

        assertThatThrownBy(() -> configService.update("ghost_key", req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("ghost_key");

        verify(configRepository, never()).save(any());
    }
}
