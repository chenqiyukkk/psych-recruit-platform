package com.project.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.common.exception.ApiException;
import com.project.common.security.JwtUtil;
import com.project.user.UserRoles;
import com.project.user.dto.WxLoginRequest;
import com.project.user.dto.WxLoginResponse;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import com.project.user.wechat.WechatMiniAppClient;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceWxLoginTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtUtil jwtUtil;
  @Mock private WechatMiniAppClient wechatMiniAppClient;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, passwordEncoder, jwtUtil, wechatMiniAppClient);
  }

  @Test
  @DisplayName("微信登录 - code 为空时抛出参数错误")
  void wxLogin_blankCode_throwsBadRequest() {
    WxLoginRequest request = new WxLoginRequest();
    request.setCode(" ");

    assertThatThrownBy(() -> userService.wxLogin(request))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("微信登录 code 不能为空");

    verify(wechatMiniAppClient, never()).getOpenid(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("微信登录 - 新 openid 自动创建被试用户并返回 token 和 profile")
  void wxLogin_newOpenid_createsSubjectUserAndReturnsTokenProfile() {
    when(wechatMiniAppClient.getOpenid("code-new")).thenReturn("openid-1234567890");
    when(userRepository.findByWechatOpenid("openid-1234567890")).thenReturn(Optional.empty());
    when(userRepository.existsByUsername("wx_34567890")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded-unusable-password");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              user.setId(10L);
              return user;
            });
    when(jwtUtil.generateToken("wx_34567890", UserRoles.SUBJECT)).thenReturn("jwt-token");

    WxLoginResponse response = userService.wxLogin(request("code-new"));

    assertThat(response.getToken()).isEqualTo("jwt-token");
    assertThat(response.getProfile().getId()).isEqualTo(10L);
    assertThat(response.getProfile().getUsername()).isEqualTo("wx_34567890");
    assertThat(response.getProfile().getRole()).isEqualTo(UserRoles.SUBJECT);
    assertThat(response.getProfile().getReputationScore()).isEqualTo(100);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getWechatOpenid()).isEqualTo("openid-1234567890");
    assertThat(saved.getPassword()).isEqualTo("encoded-unusable-password");
    assertThat(saved.getTotalReviews()).isZero();
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("微信登录 - 同一 openid 再次登录不重复创建用户")
  void wxLogin_existingOpenid_reusesUser() {
    User existing = new User();
    existing.setId(20L);
    existing.setUsername("wx_existing");
    existing.setPassword("encoded");
    existing.setWechatOpenid("openid-existing");
    existing.setRole(UserRoles.SUBJECT);
    existing.setReputationScore(100);
    existing.setTotalReviews(0);
    existing.setCreatedAt(LocalDateTime.now());

    when(wechatMiniAppClient.getOpenid("code-existing")).thenReturn("openid-existing");
    when(userRepository.findByWechatOpenid("openid-existing")).thenReturn(Optional.of(existing));
    when(jwtUtil.generateToken("wx_existing", UserRoles.SUBJECT)).thenReturn("jwt-existing");

    WxLoginResponse response = userService.wxLogin(request("code-existing"));

    assertThat(response.getToken()).isEqualTo("jwt-existing");
    assertThat(response.getProfile().getId()).isEqualTo(20L);
    assertThat(response.getProfile().getUsername()).isEqualTo("wx_existing");
    verify(userRepository, never()).save(any(User.class));
  }

  private static WxLoginRequest request(String code) {
    WxLoginRequest request = new WxLoginRequest();
    request.setCode(code);
    return request;
  }
}
