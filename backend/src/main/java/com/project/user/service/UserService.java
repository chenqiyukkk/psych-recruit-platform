package com.project.user.service;

import com.project.common.exception.ApiException;
import com.project.common.security.JwtUtil;
import com.project.user.UserRoles;
import com.project.user.dto.AuthLoginRequest;
import com.project.user.dto.AuthLoginResponse;
import com.project.user.dto.AuthRegisterRequest;
import com.project.user.dto.UserListResponse;
import com.project.user.dto.UserProfileResponse;
import com.project.user.dto.UserProfileUpdateRequest;
import com.project.user.dto.WxLoginRequest;
import com.project.user.dto.WxLoginResponse;
import com.project.user.entity.User;
import com.project.user.repo.UserRepository;
import com.project.user.wechat.WechatMiniAppClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final WechatMiniAppClient wechatMiniAppClient;

  @Transactional
  public void register(AuthRegisterRequest request) {
    if (!UserRoles.ALL.contains(request.getRole())) {
      throw new ApiException(400, "角色不合法");
    }
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new ApiException(400, "用户名已存在");
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setPhone(request.getPhone());
    user.setEmail(request.getEmail());
    user.setRole(request.getRole());
    user.setReputationScore(100);
    user.setTotalReviews(0);
    LocalDateTime now = LocalDateTime.now();
    user.setCreatedAt(now);
    userRepository.save(user);
  }

  public AuthLoginResponse login(AuthLoginRequest request) {
    User user =
        userRepository
            .findByUsername(request.getUsername())
            .orElseThrow(() -> new ApiException(401, "用户名或密码错误"));
    if ("DISABLED".equals(user.getRole())) {
      throw new ApiException(403, "该账号已被禁用，请联系管理员");
    }
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new ApiException(401, "用户名或密码错误");
    }
    String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
    return new AuthLoginResponse(token);
  }

  @Transactional
  public WxLoginResponse wxLogin(WxLoginRequest request) {
    if (request == null || !StringUtils.hasText(request.getCode())) {
      throw new ApiException(400, "微信登录 code 不能为空");
    }
    String openid = wechatMiniAppClient.getOpenid(request.getCode().trim());
    User user =
        userRepository
            .findByWechatOpenid(openid)
            .orElseGet(() -> createWechatSubject(openid));
    String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
    return new WxLoginResponse(token, toProfile(user));
  }

  public UserProfileResponse getProfile(String username) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new ApiException(404, "用户不存在"));
    return toProfile(user);
  }

  @Transactional
  public UserProfileResponse updateProfile(String username, UserProfileUpdateRequest request) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new ApiException(404, "用户不存在"));
    user.setPhone(request.getPhone());
    user.setEmail(request.getEmail());
    if (request.getGender() != null) {
      user.setGender(request.getGender());
    }
    if (request.getAgeGroup() != null) {
      user.setAgeGroup(request.getAgeGroup());
    }
    if (request.getMajorCategory() != null) {
      user.setMajorCategory(request.getMajorCategory());
    }
    if (request.getHandedness() != null) {
      user.setHandedness(request.getHandedness());
    }
    return toProfile(userRepository.save(user));
  }

  public double getResearcherRating(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(404, "用户不存在"));
    if (!UserRoles.RESEARCHER.equals(user.getRole())) {
      throw new ApiException(400, "该用户不是研究者");
    }
    if (user.getResearcherRating() == null) {
      return 0.0;
    }
    return user.getResearcherRating().doubleValue();
  }

  public List<UserListResponse> listAll() {
    return userRepository.findAll().stream()
        .map(UserListResponse::from)
        .collect(java.util.stream.Collectors.toList());
  }

  @Transactional
  public void deleteUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ApiException(404, "用户不存在"));
    if (UserRoles.ADMIN.equals(user.getRole())) {
      throw new ApiException(400, "不能删除管理员账号");
    }
    // 软禁用：改为 DISABLED 角色，保留数据满足外键约束
    user.setRole("DISABLED");
    userRepository.save(user);
  }

  @Transactional
  public void enableUser(Long userId, String role) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ApiException(404, "用户不存在"));
    if (!"DISABLED".equals(user.getRole())) {
      throw new ApiException(400, "该账号未被禁用");
    }
    if (role == null || (!UserRoles.SUBJECT.equals(role) && !UserRoles.RESEARCHER.equals(role))) {
      role = UserRoles.SUBJECT;
    }
    user.setRole(role);
    userRepository.save(user);
  }

  @Transactional
  public void resetPassword(Long userId, String newPassword) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ApiException(404, "用户不存在"));
    if (newPassword == null || newPassword.trim().length() < 6) {
      throw new ApiException(400, "密码至少6位");
    }
    user.setPassword(passwordEncoder.encode(newPassword.trim()));
    userRepository.save(user);
  }

  private User createWechatSubject(String openid) {
    User user = new User();
    user.setWechatOpenid(openid);
    user.setUsername(generateWechatUsername(openid));
    user.setPassword(passwordEncoder.encode("WECHAT_LOGIN_ONLY:" + UUID.randomUUID()));
    user.setRole(UserRoles.SUBJECT);
    user.setReputationScore(100);
    user.setTotalReviews(0);
    user.setCreatedAt(LocalDateTime.now());
    return userRepository.save(user);
  }

  private String generateWechatUsername(String openid) {
    String normalized = openid.replaceAll("[^A-Za-z0-9]", "");
    String suffix =
        normalized.length() > 8 ? normalized.substring(normalized.length() - 8) : normalized;
    if (!StringUtils.hasText(suffix)) {
      suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
    String username = "wx_" + suffix;
    if (!userRepository.existsByUsername(username)) {
      return username;
    }
    String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    return "wx_" + randomSuffix;
  }

  private static UserProfileResponse toProfile(User user) {
    UserProfileResponse r = new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getPhone(),
        user.getEmail(),
        user.getRole(),
        user.getReputationScore(),
        user.getResearcherRating(),
        user.getTotalReviews());
    r.setGender(user.getGender());
    r.setAgeGroup(user.getAgeGroup());
    r.setMajorCategory(user.getMajorCategory());
    r.setHandedness(user.getHandedness());
    return r;
  }
}
