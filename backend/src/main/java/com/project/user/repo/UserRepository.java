package com.project.user.repo;

import com.project.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  Optional<User> findByWechatOpenid(String wechatOpenid);

  boolean existsByUsername(String username);

  boolean existsByWechatOpenid(String wechatOpenid);
}

