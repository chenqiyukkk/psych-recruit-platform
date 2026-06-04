package com.project.user.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.common.exception.ApiException;
import lombok.Data;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WechatMiniAppClient {

  private static final String CODE2_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
  private static final String GRANT_TYPE = "authorization_code";

  private final WechatMiniAppProperties properties;
  private final RestTemplate restTemplate;

  public WechatMiniAppClient(
      WechatMiniAppProperties properties, RestTemplateBuilder restTemplateBuilder) {
    this.properties = properties;
    this.restTemplate = restTemplateBuilder.build();
  }

  public String getOpenid(String code) {
    if (!StringUtils.hasText(code)) {
      throw new ApiException(400, "微信登录 code 不能为空");
    }
    if (properties.isMockEnabled()) {
      return buildMockOpenid(code);
    }
    if (!StringUtils.hasText(properties.getAppId())
        || !StringUtils.hasText(properties.getAppSecret())) {
      throw new ApiException(500, "微信登录配置未完成");
    }

    Code2SessionResponse response = requestCode2Session(code.trim());
    if (response == null || !StringUtils.hasText(response.getOpenid())) {
      throw new ApiException(502, "微信登录失败，请稍后重试");
    }
    return response.getOpenid();
  }

  private Code2SessionResponse requestCode2Session(String code) {
    String url =
        UriComponentsBuilder.fromHttpUrl(CODE2_SESSION_URL)
            .queryParam("appid", properties.getAppId())
            .queryParam("secret", properties.getAppSecret())
            .queryParam("js_code", code)
            .queryParam("grant_type", GRANT_TYPE)
            .toUriString();
    try {
      Code2SessionResponse response = restTemplate.getForObject(url, Code2SessionResponse.class);
      if (response != null && response.getErrcode() != null && response.getErrcode() != 0) {
        throw new ApiException(502, "微信登录失败，请稍后重试");
      }
      return response;
    } catch (ApiException ex) {
      throw ex;
    } catch (RestClientException ex) {
      throw new ApiException(502, "微信登录失败，请稍后重试");
    }
  }

  private String buildMockOpenid(String code) {
    String sanitized = code.trim().replaceAll("[^A-Za-z0-9_-]", "_");
    if (sanitized.length() > 64) {
      sanitized = sanitized.substring(0, 64);
    }
    return properties.getMockOpenidPrefix() + sanitized;
  }

  @Data
  private static class Code2SessionResponse {
    private String openid;

    @JsonProperty("session_key")
    private String sessionKey;

    private String unionid;
    private Integer errcode;
    private String errmsg;
  }
}
