package com.project.config.repository;

import com.project.config.entity.Config;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统配置数据访问层。
 */
@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {

    /** 根据配置键查找配置项 */
    Optional<Config> findByConfigKey(String configKey);
}
