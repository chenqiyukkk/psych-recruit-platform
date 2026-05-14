package com.project.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.ApiException;
import com.project.config.controller.ConfigController;
import com.project.config.dto.ConfigUpdateRequest;
import com.project.config.entity.Config;
import com.project.config.service.ConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ConfigController MockMvc 切片测试。
 *
 * <p>使用 @WebMvcTest 只加载 Web 层，用 @MockBean 替换 ConfigService，
 * 并通过内嵌的 TestSecurityConfig 将 Spring Security 设置为"放行所有"，
 * 避免在测试中携带真实 JWT Token。
 */
@WebMvcTest(ConfigController.class)
@Import(ConfigControllerTest.TestSecurityConfig.class)
class ConfigControllerTest {

    // ----------------------------------------------------------------
    // 测试专用安全配置：放行所有请求，跳过 JWT 过滤
    // ----------------------------------------------------------------
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf().disable()
                .authorizeRequests().anyRequest().permitAll();
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfigService configService;

    // ---- 工具方法 ----

    private Config buildConfig(String key, String value) {
        Config c = new Config();
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setDescription("desc-" + key);
        return c;
    }

    // ================================================================
    // GET /api/config/experiment-types
    // ================================================================

    @Test
    @DisplayName("GET /api/config/experiment-types - 返回类型列表")
    void getExperimentTypes_returnsList() throws Exception {
        List<String> types = Arrays.asList("认知类", "情绪类", "社会类");
        when(configService.getExperimentTypes()).thenReturn(types);

        mockMvc.perform(get("/api/config/experiment-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0]").value("认知类"))
                .andExpect(jsonPath("$.data[1]").value("情绪类"))
                .andExpect(jsonPath("$.data[2]").value("社会类"));
    }

    // ================================================================
    // GET /api/config/locations
    // ================================================================

    @Test
    @DisplayName("GET /api/config/locations - 返回地点列表")
    void getLocations_returnsList() throws Exception {
        List<String> locs = Arrays.asList("逸夫楼", "图书馆");
        when(configService.getLocations()).thenReturn(locs);

        mockMvc.perform(get("/api/config/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0]").value("逸夫楼"));
    }

    // ================================================================
    // GET /api/config/tags
    // ================================================================

    @Test
    @DisplayName("GET /api/config/tags - 返回标签列表")
    void getTags_returnsList() throws Exception {
        List<String> tags = Arrays.asList("fMRI", "情绪类", "认知类");
        when(configService.getTags()).thenReturn(tags);

        mockMvc.perform(get("/api/config/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0]").value("fMRI"));
    }

    // ================================================================
    // GET /api/config  (管理员接口，测试中已放行所有权限)
    // ================================================================

    @Test
    @DisplayName("GET /api/config - 返回所有配置项")
    void listAll_returnsConfigs() throws Exception {
        List<Config> configs = Arrays.asList(
                buildConfig("experiment_types", "认知类,情绪类"),
                buildConfig("locations", "逸夫楼,图书馆")
        );
        when(configService.listAll()).thenReturn(configs);

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].configKey").value("experiment_types"));
    }

    // ================================================================
    // PUT /api/config/{key}  - 成功
    // ================================================================

    @Test
    @DisplayName("PUT /api/config/{key} - 更新成功返回更新后的配置")
    void update_success() throws Exception {
        Config updated = buildConfig("experiment_types", "认知类,情绪类,发展类");
        when(configService.update(eq("experiment_types"), any(ConfigUpdateRequest.class)))
                .thenReturn(updated);

        ConfigUpdateRequest req = new ConfigUpdateRequest();
        req.setConfigValue("认知类,情绪类,发展类");

        mockMvc.perform(put("/api/config/experiment_types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.configValue").value("认知类,情绪类,发展类"));
    }

    // ================================================================
    // PUT /api/config/{key}  - key 不存在，Service 抛 ApiException(404)
    // ================================================================

    @Test
    @DisplayName("PUT /api/config/{key} - key 不存在时返回 400（GlobalExceptionHandler 处理）")
    void update_keyNotFound_returns400() throws Exception {
        when(configService.update(eq("ghost_key"), any(ConfigUpdateRequest.class)))
                .thenThrow(new ApiException(404, "配置项不存在: ghost_key"));

        ConfigUpdateRequest req = new ConfigUpdateRequest();
        req.setConfigValue("someValue");

        mockMvc.perform(put("/api/config/ghost_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // GlobalExceptionHandler 捕获 ApiException 并返回 HTTP 400
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("配置项不存在: ghost_key"));
    }

    // ================================================================
    // PUT /api/config/{key}  - 请求体为空（参数校验失败）
    // ================================================================

    @Test
    @DisplayName("PUT /api/config/{key} - 请求体为空时返回 400（参数校验）")
    void update_emptyBody_returns400() throws Exception {
        mockMvc.perform(put("/api/config/experiment_types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                // ConfigUpdateRequest.configValue 有 @NotBlank，校验失败 → 400
                .andExpect(status().isBadRequest());
    }
}
