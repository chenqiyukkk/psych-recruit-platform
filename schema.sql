-- 心理学被试招募平台 - 数据库表结构（MySQL 8.0）
-- Engine: InnoDB, Charset: utf8mb4
-- 注意：如需自行创建数据库，请先执行：
--   CREATE DATABASE IF NOT EXISTS psych_recruit_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
--   USE psych_recruit_platform;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- 1) users 用户表
-- =========================
DROP TABLE IF EXISTS users;
CREATE TABLE users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(64) NOT NULL COMMENT '用户名（唯一）',
  password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt哈希）',
  phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  role ENUM('被试','研究者','管理员') NOT NULL COMMENT '角色：被试/研究者/管理员',
  reputation_score INT NOT NULL DEFAULT 100 COMMENT '信誉分（默认100）',
  researcher_rating DECIMAL(3,2) DEFAULT NULL COMMENT '研究者平均评分（缓存字段）',
  total_reviews INT NOT NULL DEFAULT 0 COMMENT '被评价次数（缓存字段）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  KEY idx_users_role (role),
  KEY idx_users_phone (phone),
  KEY idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- =========================
-- 2) participant_profiles 被试档案（匿名化）
-- =========================
DROP TABLE IF EXISTS participant_profiles;
CREATE TABLE participant_profiles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID（被试）',
  anonymous_id VARCHAR(32) NOT NULL COMMENT '匿名编号（如 P-2025-001）',
  age_group VARCHAR(32) DEFAULT NULL COMMENT '年龄段',
  gender VARCHAR(32) DEFAULT NULL COMMENT '性别',
  major_category VARCHAR(64) DEFAULT NULL COMMENT '专业大类',
  handedness VARCHAR(32) DEFAULT NULL COMMENT '惯用手',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_participant_profiles_user (user_id),
  UNIQUE KEY uk_participant_profiles_anonymous_id (anonymous_id),
  KEY idx_participant_profiles_age_group (age_group),
  KEY idx_participant_profiles_gender (gender),
  CONSTRAINT fk_participant_profiles_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='被试档案（匿名化）';

-- =========================
-- 3) experiments 实验项目
-- =========================
DROP TABLE IF EXISTS experiments;
CREATE TABLE experiments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(200) NOT NULL COMMENT '实验标题',
  description TEXT DEFAULT NULL COMMENT '实验描述',
  location VARCHAR(255) DEFAULT NULL COMMENT '地点/线上链接说明',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  ethics_approval_no VARCHAR(128) DEFAULT NULL COMMENT '伦理审批编号',
  risk_level ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'LOW' COMMENT '风险等级',
  payment_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '报酬金额',
  payment_method ENUM('OFFLINE','ONLINE') NOT NULL DEFAULT 'OFFLINE' COMMENT '报酬方式：线下/线上（当前以线下为主）',
  payment_description VARCHAR(255) DEFAULT NULL COMMENT '报酬说明',
  screening_criteria JSON DEFAULT NULL COMMENT '筛选条件（JSON）',
  exclude_tags JSON DEFAULT NULL COMMENT '互斥标签（JSON）',
  status ENUM('DRAFT','PUBLISHED','RECRUITING','FULL','ONGOING','COMPLETED') NOT NULL DEFAULT 'DRAFT' COMMENT '实验状态',
  organizer_id BIGINT UNSIGNED NOT NULL COMMENT '组织者（研究者）ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_experiments_status (status),
  KEY idx_experiments_time (start_time, end_time),
  KEY idx_experiments_organizer (organizer_id),
  CONSTRAINT fk_experiments_organizer
    FOREIGN KEY (organizer_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验项目';

-- =========================
-- 4) experiment_tags 实验标签（含冷却期）
-- =========================
DROP TABLE IF EXISTS experiment_tags;
CREATE TABLE experiment_tags (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  experiment_id BIGINT UNSIGNED NOT NULL COMMENT '实验ID',
  tag_name VARCHAR(64) NOT NULL COMMENT '标签名',
  cooling_days INT NOT NULL DEFAULT 0 COMMENT '冷却天数（互斥/冷却逻辑用）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_experiment_tags_exp_tag (experiment_id, tag_name),
  KEY idx_experiment_tags_tag (tag_name),
  CONSTRAINT fk_experiment_tags_experiment
    FOREIGN KEY (experiment_id) REFERENCES experiments(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验标签';

-- =========================
-- 5) registrations 报名记录
-- =========================
DROP TABLE IF EXISTS registrations;
CREATE TABLE registrations (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  experiment_id BIGINT UNSIGNED NOT NULL COMMENT '实验ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '报名用户ID（被试）',
  status ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '报名状态',
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  reviewed_at DATETIME DEFAULT NULL COMMENT '审核时间',
  sign_in_time DATETIME DEFAULT NULL COMMENT '签到时间',
  is_completed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成实验',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_registrations_exp_user (experiment_id, user_id),
  KEY idx_registrations_user (user_id),
  KEY idx_registrations_experiment (experiment_id),
  KEY idx_registrations_status (status),
  CONSTRAINT fk_registrations_experiment
    FOREIGN KEY (experiment_id) REFERENCES experiments(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_registrations_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报名记录';

-- =========================
-- 6) consent_records 知情同意书签署记录
-- =========================
DROP TABLE IF EXISTS consent_records;
CREATE TABLE consent_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  registration_id BIGINT UNSIGNED NOT NULL COMMENT '报名记录ID',
  consent_version VARCHAR(64) NOT NULL COMMENT '同意书版本号',
  signed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签署时间',
  ip_address VARCHAR(64) DEFAULT NULL COMMENT '签署IP',
  device_fingerprint VARCHAR(255) DEFAULT NULL COMMENT '设备指纹',
  is_verified TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已验证',
  PRIMARY KEY (id),
  UNIQUE KEY uk_consent_records_registration (registration_id),
  KEY idx_consent_records_signed_at (signed_at),
  CONSTRAINT fk_consent_records_registration
    FOREIGN KEY (registration_id) REFERENCES registrations(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知情同意书签署记录';

-- =========================
-- 7) payment_codes 收款码（研究者/被试都可能配置）
-- =========================
DROP TABLE IF EXISTS payment_codes;
CREATE TABLE payment_codes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  payment_type ENUM('WECHAT','ALIPAY') NOT NULL COMMENT '收款类型',
  qr_code_url VARCHAR(1024) NOT NULL COMMENT '收款码图片URL/路径',
  is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认收款码',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_payment_codes_user (user_id),
  KEY idx_payment_codes_user_default (user_id, is_default),
  CONSTRAINT fk_payment_codes_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收款码';

-- =========================
-- 8) payment_records 支付记录（线下支付确认）
-- =========================
DROP TABLE IF EXISTS payment_records;
CREATE TABLE payment_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  registration_id BIGINT UNSIGNED NOT NULL COMMENT '报名记录ID',
  payer_user_id BIGINT UNSIGNED NOT NULL COMMENT '付款方用户ID（研究者）',
  payee_user_id BIGINT UNSIGNED NOT NULL COMMENT '收款方用户ID（被试）',
  amount DECIMAL(10,2) NOT NULL COMMENT '金额',
  payment_screenshot_url VARCHAR(1024) DEFAULT NULL COMMENT '支付凭证截图URL/路径',
  payer_confirmed_at DATETIME DEFAULT NULL COMMENT '付款方确认时间',
  payee_confirmed_at DATETIME DEFAULT NULL COMMENT '收款方确认时间',
  status ENUM('PENDING','PAID','CONFIRMED','DISPUTED') NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_records_registration (registration_id),
  KEY idx_payment_records_status (status),
  KEY idx_payment_records_payer (payer_user_id),
  KEY idx_payment_records_payee (payee_user_id),
  CONSTRAINT fk_payment_records_registration
    FOREIGN KEY (registration_id) REFERENCES registrations(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_payment_records_payer
    FOREIGN KEY (payer_user_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_payment_records_payee
    FOREIGN KEY (payee_user_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录（线下确认）';

-- =========================
-- 9) reviews 双向评价记录
-- =========================
DROP TABLE IF EXISTS reviews;
CREATE TABLE reviews (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  registration_id BIGINT UNSIGNED NOT NULL COMMENT '报名记录ID',
  reviewer_id BIGINT UNSIGNED NOT NULL COMMENT '评价人ID',
  reviewed_id BIGINT UNSIGNED NOT NULL COMMENT '被评价人ID',
  review_type ENUM('SUBJECT_TO_RESEARCHER','RESEARCHER_TO_SUBJECT') NOT NULL COMMENT '评价方向',
  rating INT NOT NULL COMMENT '总体评分（1-5）',
  communication_score INT DEFAULT NULL COMMENT '沟通评分（1-5）',
  professionalism_score INT DEFAULT NULL COMMENT '专业评分（1-5）',
  punctuality_score INT DEFAULT NULL COMMENT '守时评分（1-5）',
  comment TEXT DEFAULT NULL COMMENT '评价内容',
  is_anonymous TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否匿名',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_reviews_registration_type (registration_id, review_type),
  KEY idx_reviews_reviewed (reviewed_id),
  KEY idx_reviews_reviewer (reviewer_id),
  KEY idx_reviews_type (review_type),
  CONSTRAINT fk_reviews_registration
    FOREIGN KEY (registration_id) REFERENCES registrations(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_reviews_reviewer
    FOREIGN KEY (reviewer_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_reviews_reviewed
    FOREIGN KEY (reviewed_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='双向评价记录';

-- =========================
-- 10) appeals 申诉记录
-- =========================
DROP TABLE IF EXISTS appeals;
CREATE TABLE appeals (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  appellant_id BIGINT UNSIGNED NOT NULL COMMENT '申诉人ID',
  appeal_type ENUM('REPUTATION_DEDUCTION','LOW_RATING','PAYMENT_DISPUTE') NOT NULL COMMENT '申诉类型',
  target_id BIGINT UNSIGNED NOT NULL COMMENT '关联记录ID（如 reputation_logs/payment_records/reviews 的 id）',
  reason TEXT NOT NULL COMMENT '申诉理由',
  evidence_urls JSON DEFAULT NULL COMMENT '证据URL列表（JSON）',
  status ENUM('PENDING','UNDER_REVIEW','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '申诉状态',
  reviewer_id BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人（管理员）ID',
  review_comment TEXT DEFAULT NULL COMMENT '审核意见',
  reviewed_at DATETIME DEFAULT NULL COMMENT '审核时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_appeals_appellant (appellant_id),
  KEY idx_appeals_status (status),
  KEY idx_appeals_type_target (appeal_type, target_id),
  KEY idx_appeals_reviewer (reviewer_id),
  CONSTRAINT fk_appeals_appellant
    FOREIGN KEY (appellant_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_appeals_reviewer
    FOREIGN KEY (reviewer_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申诉记录';

-- =========================
-- 11) reputation_logs 信誉分记录
-- =========================
DROP TABLE IF EXISTS reputation_logs;
CREATE TABLE reputation_logs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  change_type VARCHAR(64) NOT NULL COMMENT '变更类型（如 NO_SHOW/LATE_CANCEL/ADMIN_ADJUST 等）',
  score_delta INT NOT NULL COMMENT '分数变化（可正可负）',
  reason VARCHAR(255) DEFAULT NULL COMMENT '原因说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_reputation_logs_user (user_id),
  KEY idx_reputation_logs_created_at (created_at),
  CONSTRAINT fk_reputation_logs_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信誉分记录';

-- =========================
-- 12) notifications 系统通知
-- =========================
DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '接收用户ID',
  title VARCHAR(100) NOT NULL COMMENT '通知标题',
  content TEXT NOT NULL COMMENT '通知内容',
  type VARCHAR(50) NOT NULL COMMENT '通知类型（REGISTRATION_APPROVED/REGISTRATION_REJECTED/PAYMENT_CONFIRMED/EXPERIMENT_STARTED/REVIEW_RECEIVED/APPEAL_PROCESSED）',
  related_type VARCHAR(50) DEFAULT NULL COMMENT '关联业务类型（experiment/registration/payment/review/appeal）',
  related_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务记录ID',
  is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读（0未读/1已读）',
  read_at DATETIME DEFAULT NULL COMMENT '阅读时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_notifications_user_id (user_id),
  KEY idx_notifications_type (type),
  KEY idx_notifications_is_read (is_read),
  KEY idx_notifications_created_at (created_at),
  CONSTRAINT fk_notifications_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知表';

-- =========================
-- 13) configs 系统配置
-- =========================
DROP TABLE IF EXISTS configs;
CREATE TABLE configs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  config_key VARCHAR(100) NOT NULL COMMENT '配置键（唯一）',
  config_value JSON NOT NULL COMMENT '配置值（JSON）',
  description VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
  category VARCHAR(50) DEFAULT NULL COMMENT '配置分类（EXPERIMENT/LOCATION/TAG/SYSTEM）',
  is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0禁用/1启用）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_configs_key (config_key),
  KEY idx_configs_category (category),
  KEY idx_configs_is_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

SET FOREIGN_KEY_CHECKS = 1;
