"""
测试数据生成脚本
使用 Faker 库生成用户、被试档案、实验项目等测试数据并插入 MySQL 数据库。

依赖安装：
    pip install faker mysql-connector-python

使用方式：
    python scripts/generate_test_data.py

数据库连接配置可通过命令行参数覆盖，例如：
    python scripts/generate_test_data.py --host 127.0.0.1 --port 3306 \
        --user root --password yourpwd --database psychology_platform
"""

import argparse
import hashlib
import json
import random
import sys
from datetime import datetime, timedelta

# ---------------------------------------------------------------------------
# 依赖检查
# ---------------------------------------------------------------------------
try:
    from faker import Faker
except ImportError:
    print("[ERROR] 未安装 faker 库，请先执行: pip install faker")
    sys.exit(1)

try:
    import mysql.connector
    from mysql.connector import Error as MySQLError
except ImportError:
    print("[ERROR] 未安装 mysql-connector-python 库，请先执行: pip install mysql-connector-python")
    sys.exit(1)

# ---------------------------------------------------------------------------
# 常量定义（与后端枚举对齐）
# ---------------------------------------------------------------------------
ROLES = ["SUBJECT", "RESEARCHER", "ADMIN"]
GENDERS = ["MALE", "FEMALE", "OTHER"]
AGE_GROUPS = ["18-22", "23-25", "26-30", "31-35"]
MAJOR_CATEGORIES = ["心理学类", "计算机类", "文学类", "理学类", "工学类", "医学类", "经管类"]
HANDEDNESS = ["LEFT", "RIGHT", "MIXED"]
RISK_LEVELS = ["LOW", "MEDIUM", "HIGH"]
EXPERIMENT_STATUSES = ["DRAFT", "PUBLISHED", "RECRUITING", "FULL", "ONGOING", "COMPLETED"]
PAYMENT_METHODS = ["OFFLINE"]
APPEAL_TYPES = ["REPUTATION_DEDUCTION", "LOW_RATING", "PAYMENT_DISPUTE"]
APPEAL_STATUSES = ["PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED"]
NOTIFICATION_TYPES = [
    "REGISTRATION_APPROVED",
    "REGISTRATION_REJECTED",
    "PAYMENT_CONFIRMED",
    "EXPERIMENT_STARTED",
    "REVIEW_RECEIVED",
    "APPEAL_PROCESSED",
]
EXPERIMENT_TAGS = ["fMRI", "情绪类", "认知类", "社会类", "发展类", "临床类", "行为类"]
CONFIG_KEYS = [
    ("activity_types", '["认知", "社会", "临床", "发展", "行为"]', "实验类型列表"),
    ("locations", '["心理学楼101", "心理学楼201", "实验中心A栋", "图书馆报告厅"]', "常用地点列表"),
    ("experiment_tags", '["fMRI", "情绪类", "认知类", "社会类", "发展类", "临床类", "行为类"]', "实验标签列表"),
    ("major_categories", '["心理学类", "计算机类", "文学类", "理学类", "工学类", "医学类", "经管类"]', "专业类别列表"),
    ("reputation_threshold", "60", "最低信誉分阈值"),
    ("payment_auto_confirm_days", "7", "支付超时自动确认天数"),
    ("experiment_remind_hours", "24", "实验开始前提醒小时数"),
]

fake = Faker("zh_CN")
Faker.seed(42)
random.seed(42)


# ---------------------------------------------------------------------------
# 数据生成函数
# ---------------------------------------------------------------------------

def _bcrypt_placeholder(plain: str) -> str:
    """
    生产环境应使用 BCrypt，此处生成一个格式合法的占位哈希（SHA-256 前缀）。
    实际插入数据库后如需登录，请在后端用相同的明文密码重新注册，
    或直接在数据库中用 BCrypt 工具替换该字段。
    """
    sha = hashlib.sha256(plain.encode()).hexdigest()
    # 模拟 BCrypt 格式前缀，让后端能识别（长度 60 位以上）
    return f"$2a$10${sha[:53]}"


def generate_users(count: int = 100):
    """生成测试用户列表。返回 dict list，不含自增 id。"""
    users = []
    # 强制生成各角色代表账号，方便手动测试
    seeded = [
        ("subject_test", "SUBJECT"),
        ("researcher_test", "RESEARCHER"),
        ("admin_test", "ADMIN"),
    ]
    for uname, role in seeded:
        users.append({
            "username": uname,
            "password": _bcrypt_placeholder("123456"),
            "phone": fake.phone_number()[:32],
            "email": fake.email()[:128],
            "role": role,
            "reputation_score": 100 if role == "SUBJECT" else None,
            "researcher_rating": None,
            "total_reviews": 0,
            "created_at": datetime.now() - timedelta(days=random.randint(1, 365)),
        })

    for i in range(count - len(seeded)):
        role = random.choices(ROLES, weights=[70, 25, 5])[0]
        researcher_rating = (
            round(random.uniform(3.0, 5.0), 2) if role == "RESEARCHER" else None
        )
        users.append({
            "username": f"user_{i:04d}_{fake.user_name()[:20]}",
            "password": _bcrypt_placeholder("123456"),
            "phone": fake.phone_number()[:32],
            "email": fake.email()[:128],
            "role": role,
            "reputation_score": random.randint(40, 100) if role == "SUBJECT" else None,
            "researcher_rating": researcher_rating,
            "total_reviews": random.randint(0, 50) if role == "RESEARCHER" else 0,
            "created_at": datetime.now() - timedelta(days=random.randint(1, 365)),
        })
    return users


def generate_participant_profiles(user_id_role_map: dict, count: int = 80):
    """
    生成被试档案。
    :param user_id_role_map: {user_id: role}
    :param count: 生成条数
    """
    subject_ids = [uid for uid, role in user_id_role_map.items() if role == "SUBJECT"]
    if not subject_ids:
        print("[WARN] 没有找到 SUBJECT 用户，跳过被试档案生成")
        return []

    profiles = []
    used_anon_ids = set()
    for i in range(min(count, len(subject_ids))):
        user_id = subject_ids[i]
        anon_seq = i + 1
        anon_id = f"P-2025-{anon_seq:03d}"
        while anon_id in used_anon_ids:
            anon_seq += 1
            anon_id = f"P-2025-{anon_seq:03d}"
        used_anon_ids.add(anon_id)

        profiles.append({
            "user_id": user_id,
            "anonymous_id": anon_id,
            "age_group": random.choice(AGE_GROUPS),
            "gender": random.choice(GENDERS),
            "major_category": random.choice(MAJOR_CATEGORIES),
            "handedness": random.choice(HANDEDNESS),
            "created_at": datetime.now() - timedelta(days=random.randint(1, 300)),
        })
    return profiles


def generate_experiments(researcher_ids: list, count: int = 50):
    """生成实验项目列表。"""
    if not researcher_ids:
        print("[WARN] 没有找到研究者用户，跳过实验生成")
        return []

    experiments = []
    for i in range(count):
        delta_days = random.randint(-30, 60)
        start_time = datetime.now() + timedelta(days=delta_days, hours=random.randint(0, 12))
        end_time = start_time + timedelta(hours=random.randint(1, 8))

        # 筛选条件 JSON
        min_age = random.randint(18, 22)
        max_age = min_age + random.randint(3, 12)
        screening = {
            "include": {
                "age_range": [min_age, max_age],
                "gender": random.choice(["MALE", "FEMALE", "ANY"]),
            }
        }

        # 互斥标签 JSON
        n_tags = random.randint(0, 3)
        exclude = random.sample(EXPERIMENT_TAGS, n_tags)

        experiments.append({
            "title": fake.sentence(nb_words=6)[:200],
            "description": fake.paragraph(nb_sentences=5),
            "location": random.choice(["心理学楼101", "心理学楼201", "实验中心A栋", "图书馆报告厅"])[:255],
            "start_time": start_time,
            "end_time": end_time,
            "ethics_approval_no": f"IRB-2025-PSY-{i+1:03d}"[:128],
            "risk_level": random.choice(RISK_LEVELS),
            "payment_amount": round(random.uniform(20.0, 200.0), 2),
            "payment_method": "OFFLINE",
            "payment_description": "实验结束后现场支付"[:255],
            "screening_criteria": json.dumps(screening, ensure_ascii=False),
            "exclude_tags": json.dumps(exclude, ensure_ascii=False),
            "status": random.choice(EXPERIMENT_STATUSES),
            "organizer_id": random.choice(researcher_ids),
            "created_at": datetime.now() - timedelta(days=random.randint(1, 90)),
            "updated_at": datetime.now() - timedelta(days=random.randint(0, 30)),
        })
    return experiments


def generate_experiment_tags(experiment_ids: list):
    """为每个实验随机生成 0-3 条实验标签记录。"""
    tags = []
    for exp_id in experiment_ids:
        n = random.randint(0, 3)
        chosen = random.sample(EXPERIMENT_TAGS, n)
        for tag_name in chosen:
            tags.append({
                "experiment_id": exp_id,
                "tag_name": tag_name,
                "cooling_days": random.choice([14, 30, 60]),
            })
    return tags


def generate_notifications(user_ids: list, count: int = 200):
    """生成站内通知记录。"""
    if not user_ids:
        return []
    notifications = []
    for _ in range(count):
        ntype = random.choice(NOTIFICATION_TYPES)
        is_read = random.choice([True, False])
        created_at = datetime.now() - timedelta(days=random.randint(0, 30))
        read_at = created_at + timedelta(hours=random.randint(1, 48)) if is_read else None
        notifications.append({
            "user_id": random.choice(user_ids),
            "title": f"【{ntype}】系统通知",
            "content": fake.sentence(nb_words=10),
            "type": ntype,
            "related_type": random.choice(["experiment", "registration", "payment", "review", "appeal", None]),
            "related_id": random.randint(1, 50) if random.random() > 0.3 else None,
            "is_read": is_read,
            "read_at": read_at,
            "created_at": created_at,
        })
    return notifications


def generate_configs():
    """生成系统配置数据（幂等：key 唯一）。"""
    return [
        {
            "config_key": key,
            "config_value": value,
            "description": desc,
        }
        for key, value, desc in CONFIG_KEYS
    ]


# ---------------------------------------------------------------------------
# 数据库插入函数
# ---------------------------------------------------------------------------

def _exec(cursor, sql: str, params=None):
    cursor.execute(sql, params or ())


def insert_users(cursor, users: list) -> dict:
    """插入用户，返回 {username: (id, role)} 映射。"""
    sql = """
        INSERT INTO users
            (username, password, phone, email, role,
             reputation_score, researcher_rating, total_reviews, created_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            phone = VALUES(phone),
            email = VALUES(email),
            updated_at = NOW()
    """
    # users 表可能没有 updated_at，改成忽略重复
    sql_ignore = """
        INSERT IGNORE INTO users
            (username, password, phone, email, role,
             reputation_score, researcher_rating, total_reviews, created_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    id_role_map = {}
    for u in users:
        _exec(cursor, sql_ignore, (
            u["username"], u["password"], u["phone"], u["email"], u["role"],
            u["reputation_score"], u["researcher_rating"], u["total_reviews"], u["created_at"],
        ))
        if cursor.lastrowid:
            id_role_map[cursor.lastrowid] = u["role"]
    return id_role_map


def insert_participant_profiles(cursor, profiles: list):
    sql = """
        INSERT IGNORE INTO participant_profiles
            (user_id, anonymous_id, age_group, gender, major_category, handedness, created_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
    """
    for p in profiles:
        _exec(cursor, sql, (
            p["user_id"], p["anonymous_id"], p["age_group"],
            p["gender"], p["major_category"], p["handedness"], p["created_at"],
        ))


def insert_experiments(cursor, experiments: list) -> list:
    sql = """
        INSERT INTO experiments
            (title, description, location, start_time, end_time,
             ethics_approval_no, risk_level, payment_amount, payment_method,
             payment_description, screening_criteria, exclude_tags,
             status, organizer_id, created_at, updated_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    ids = []
    for e in experiments:
        _exec(cursor, sql, (
            e["title"], e["description"], e["location"],
            e["start_time"], e["end_time"], e["ethics_approval_no"],
            e["risk_level"], e["payment_amount"], e["payment_method"],
            e["payment_description"], e["screening_criteria"], e["exclude_tags"],
            e["status"], e["organizer_id"], e["created_at"], e["updated_at"],
        ))
        ids.append(cursor.lastrowid)
    return ids


def insert_experiment_tags(cursor, tags: list):
    sql = """
        INSERT IGNORE INTO experiment_tags (experiment_id, tag_name, cooling_days)
        VALUES (%s, %s, %s)
    """
    for t in tags:
        _exec(cursor, sql, (t["experiment_id"], t["tag_name"], t["cooling_days"]))


def insert_notifications(cursor, notifications: list):
    sql = """
        INSERT INTO notifications
            (user_id, title, content, type, related_type, related_id,
             is_read, read_at, created_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    for n in notifications:
        _exec(cursor, sql, (
            n["user_id"], n["title"], n["content"], n["type"],
            n["related_type"], n["related_id"],
            n["is_read"], n["read_at"], n["created_at"],
        ))


def insert_configs(cursor, configs: list):
    sql = """
        INSERT IGNORE INTO configs (config_key, config_value, description)
        VALUES (%s, %s, %s)
    """
    for c in configs:
        _exec(cursor, sql, (c["config_key"], c["config_value"], c["description"]))


# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------

def parse_args():
    parser = argparse.ArgumentParser(description="心理学被试招募平台 - 测试数据生成工具")
    parser.add_argument("--host", default="127.0.0.1", help="MySQL 主机（默认 127.0.0.1）")
    parser.add_argument("--port", type=int, default=3306, help="MySQL 端口（默认 3306）")
    parser.add_argument("--user", default="root", help="MySQL 用户名（默认 root）")
    parser.add_argument("--password", default="123456", help="MySQL 密码（默认 123456）")
    parser.add_argument("--database", default="psychology_platform", help="数据库名（默认 psychology_platform）")
    parser.add_argument("--user-count", type=int, default=100, help="生成用户数量（默认 100）")
    parser.add_argument("--exp-count", type=int, default=50, help="生成实验数量（默认 50）")
    parser.add_argument("--notify-count", type=int, default=200, help="生成通知数量（默认 200）")
    parser.add_argument("--dry-run", action="store_true", help="仅打印将要生成的数据量，不实际写入数据库")
    return parser.parse_args()


def main():
    args = parse_args()

    # ------ 生成数据 ------
    print("正在生成测试数据...")
    users = generate_users(args.user_count)
    configs = generate_configs()

    if args.dry_run:
        print(f"[DRY RUN] 将生成：")
        print(f"  - {len(users)} 个用户")
        print(f"  - 最多 80 个被试档案")
        print(f"  - {args.exp_count} 个实验项目")
        print(f"  - {args.notify_count} 条通知")
        print(f"  - {len(configs)} 条系统配置")
        return

    # ------ 连接数据库 ------
    print(f"连接数据库 {args.user}@{args.host}:{args.port}/{args.database} ...")
    try:
        conn = mysql.connector.connect(
            host=args.host,
            port=args.port,
            user=args.user,
            password=args.password,
            database=args.database,
            charset="utf8mb4",
        )
    except MySQLError as e:
        print(f"[ERROR] 数据库连接失败：{e}")
        print("请检查数据库连接参数，或使用 --host / --port / --user / --password / --database 指定。")
        sys.exit(1)

    cursor = conn.cursor()

    try:
        # 1. 插入用户
        print(f"  插入 {len(users)} 个用户...")
        id_role_map = insert_users(cursor, users)
        conn.commit()

        # 若 INSERT IGNORE 导致 lastrowid=0（已存在），查询实际 ID
        if len(id_role_map) < len(users):
            cursor.execute("SELECT id, role FROM users ORDER BY id")
            rows = cursor.fetchall()
            id_role_map = {row[0]: row[1] for row in rows}

        print(f"    已获取 {len(id_role_map)} 个用户 ID")

        # 2. 插入被试档案
        profiles = generate_participant_profiles(id_role_map, count=80)
        print(f"  插入 {len(profiles)} 个被试档案...")
        insert_participant_profiles(cursor, profiles)
        conn.commit()

        # 3. 插入实验
        researcher_ids = [uid for uid, role in id_role_map.items() if role == "RESEARCHER"]
        experiments = generate_experiments(researcher_ids, count=args.exp_count)
        print(f"  插入 {len(experiments)} 个实验项目...")
        experiment_ids = insert_experiments(cursor, experiments)
        conn.commit()

        # 4. 插入实验标签
        exp_tags = generate_experiment_tags(experiment_ids)
        print(f"  插入 {len(exp_tags)} 条实验标签...")
        insert_experiment_tags(cursor, exp_tags)
        conn.commit()

        # 5. 插入通知
        all_user_ids = list(id_role_map.keys())
        notifications = generate_notifications(all_user_ids, count=args.notify_count)
        print(f"  插入 {len(notifications)} 条通知...")
        insert_notifications(cursor, notifications)
        conn.commit()

        # 6. 插入系统配置
        print(f"  插入 {len(configs)} 条系统配置...")
        insert_configs(cursor, configs)
        conn.commit()

    except MySQLError as e:
        conn.rollback()
        print(f"[ERROR] 数据插入失败，已回滚：{e}")
        sys.exit(1)
    finally:
        cursor.close()
        conn.close()

    print("\n✅ 测试数据生成完成！")
    print(f"  用户数：{len(users)}")
    print(f"  被试档案：{len(profiles)}")
    print(f"  实验项目：{len(experiments)}")
    print(f"  实验标签：{len(exp_tags)}")
    print(f"  通知记录：{len(notifications)}")
    print(f"  系统配置：{len(configs)}")
    print(f"\n所有账号的默认密码均为 123456（已哈希占位，如需真正登录请通过接口注册）。")
    print("测试专用账号：subject_test / researcher_test / admin_test，密码均为 123456。")


if __name__ == "__main__":
    main()
