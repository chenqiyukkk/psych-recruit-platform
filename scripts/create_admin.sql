-- 创建管理员账号
-- 用户名: admin  密码: admin123
INSERT INTO users (username, password, role, reputation_score)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 100)
ON DUPLICATE KEY UPDATE role='ADMIN';
