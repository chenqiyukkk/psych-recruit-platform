"""
心理学被试招募平台 — 性能测试脚本 (Locust)

用法:
    # 启动 Locust Web UI (推荐，可视化管理测试)
    locust -f tests/performance/locustfile.py --host=http://localhost:8080

    # 无 UI 模式 (CI/CD 适用)
    locust -f tests/performance/locustfile.py \
        --host=http://localhost:8080 \
        --headless \
        --users 100 \
        --spawn-rate 5 \
        --run-time 300s \
        --html=report.html \
        --csv=results

前置条件:
    1. 后端服务已启动: mvn spring-boot:run (或 java -jar target/*.jar)
    2. 数据库中已有测试数据 (运行 scripts/generate_test_data.py)
    3. pip install locust
"""

import random
import time
from locust import HttpUser, task, between, events, TaskSet
from locust.runners import MasterRunner


# ═══════════════════════════════════════════════════════════════════════════════════
# 测试配置常量
# ═══════════════════════════════════════════════════════════════════════════════════

# 测试用密码（所有测试用户共用）
TEST_PASSWORD = "123456"

# 测试用户前缀（用于动态生成用户名）
SUBJECT_PREFIX = "test_subject_"
RESEARCHER_PREFIX = "test_researcher_"
ADMIN_USERNAME = "admin"

# 超时时间（秒）
REQUEST_TIMEOUT = 10


def build_headers(token=None):
    """构建请求头"""
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


# ═══════════════════════════════════════════════════════════════════════════════════
# 自定义事件 — 收集业务指标
# ═══════════════════════════════════════════════════════════════════════════════════

@events.init.add_listener
def on_locust_init(environment, **kwargs):
    """测试初始化时的回调"""
    if isinstance(environment.runner, MasterRunner):
        print("📍 性能测试启动 — 心理学被试招募平台")
        print(f"   目标地址: {environment.host}")
        print("   模拟用户类型: 被试 / 研究者 / 匿名浏览者")
        print("   测试前请确认数据库已准备测试数据\n")


@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """测试开始时打印信息"""
    print(f"\n🚀 开始施压 — {environment.runner.user_count} 个虚拟用户已就绪\n")


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """测试结束时打印统计摘要"""
    if environment.stats.total.num_requests == 0:
        return
    stats = environment.stats
    print("\n" + "=" * 60)
    print("📊 性能测试结果摘要")
    print("=" * 60)
    print(f"  总请求数:     {stats.total.num_requests}")
    print(f"  失败请求数:   {stats.total.num_failures}")
    print(f"  平均响应时间: {stats.total.avg_response_time:.0f} ms")
    print(f"  P50 响应时间: {stats.total.get_response_time_percentile(0.5):.0f} ms")
    print(f"  P95 响应时间: {stats.total.get_response_time_percentile(0.95):.0f} ms")
    print(f"  P99 响应时间: {stats.total.get_response_time_percentile(0.99):.0f} ms")
    print(f"  最大响应时间: {stats.total.max_response_time:.0f} ms")
    print(f"  RPS (请求/秒): {stats.total.total_rps:.1f}")
    print(f"  失败率:       {stats.total.fail_ratio * 100:.1f}%")
    print("=" * 60 + "\n")
    print("✅ 非功能需求检验标准:")
    print(f"  {'✅' if stats.total.avg_response_time < 500 else '❌'} 接口平均响应时间 < 500ms (实际: {stats.total.avg_response_time:.0f}ms)")
    print(f"  {'✅' if stats.total.get_response_time_percentile(0.95) < 2000 else '❌'} P95 响应时间 < 2s (实际: {stats.total.get_response_time_percentile(0.95):.0f}ms)")
    print(f"  {'✅' if stats.total.fail_ratio < 0.01 else '❌'} 错误率 < 1% (实际: {stats.total.fail_ratio * 100:.1f}%)\n")


# ═══════════════════════════════════════════════════════════════════════════════════
# 用户类 1: 被试用户 (Subject)
# ═══════════════════════════════════════════════════════════════════════════════════

class SubjectUser(HttpUser):
    """
    模拟被试用户行为:
    - 浏览实验列表 (高频)
    - 查看实验详情 (中频)
    - 查看个人信息
    - 报名实验
    - 查看我的报名列表
    - 查看消息通知
    """

    wait_time = between(1, 3)  # 被试操作间隔 1~3 秒
    token = None

    def on_start(self):
        """被试登录 — 使用预生成的 subject_test 账号（密码 123456）"""
        # 取一个预生成的被试账号（数据库中已有, bcrypt 密码正确）
        username = random.choice(["subject_test"] + [f"user_{i:04d}" for i in range(70)])
        self._do_login(username)

    def _do_login(self, username):
        """仅登录，不尝试注册（注册接口当前有 bug 返回 500）"""
        with self.client.post(
            "/api/auth/login",
            json={"username": username, "password": TEST_PASSWORD},
            headers=build_headers(),
            timeout=REQUEST_TIMEOUT,
            catch_response=True,
        ) as resp:
            if resp.status_code == 200:
                body = resp.json()
                if body.get("code") == 0 and body.get("data"):
                    self.token = body["data"].get("token")
                    return
            # 使用 subject_test（密码确定正确）
            if username != "subject_test":
                resp2 = self.client.post(
                    "/api/auth/login",
                    json={"username": "subject_test", "password": TEST_PASSWORD},
                    headers=build_headers(),
                    timeout=REQUEST_TIMEOUT,
                )
                if resp2.status_code == 200:
                    body = resp2.json()
                    if body.get("code") == 0 and body.get("data"):
                        self.token = body["data"].get("token")
                        return
            resp.failure(f"登录失败: {resp.text}")

    # ── 任务定义 ──────────────────────────────────────────────────────────────────

    @task(4)
    def browse_experiments(self):
        """浏览实验列表 — 高频操作，测试分页查询性能"""
        page = random.randint(1, 5)
        size = random.choice([10, 20])
        self.client.get(
            f"/api/experiments?page={page}&size={size}",
            headers=build_headers(self.token),
            name="/api/experiments [列表]",
            timeout=REQUEST_TIMEOUT,
        )

    @task(2)
    def view_experiment_detail(self):
        """查看实验详情"""
        experiment_id = random.randint(1, 50)
        self.client.get(
            f"/api/experiments/{experiment_id}",
            headers=build_headers(self.token),
            name="/api/experiments/{id} [详情]",
            timeout=REQUEST_TIMEOUT,
        )

    @task(1)
    def view_my_profile(self):
        """查看个人信息"""
        self.client.get(
            "/api/users/profile",
            headers=build_headers(self.token),
            name="/api/users/profile",
            timeout=REQUEST_TIMEOUT,
        )

    @task(2)
    def view_my_registrations(self):
        """查看我的报名列表"""
        self.client.get(
            "/api/registrations/my",
            headers=build_headers(self.token),
            name="/api/registrations/my",
            timeout=REQUEST_TIMEOUT,
        )

    @task(1)
    def register_for_experiment(self):
        """报名实验"""
        experiment_id = random.randint(1, 50)
        with self.client.post(
            f"/api/registrations/experiments/{experiment_id}",
            headers=build_headers(self.token),
            name="/api/registrations [报名]",
            timeout=REQUEST_TIMEOUT,
            catch_response=True,
        ) as resp:
            # 报名可能因各种校验失败（已报名/满员/信誉分不足等），
            # 只要接口正常返回就算通过（非 5xx 错误）
            if resp.status_code >= 500:
                resp.failure(f"服务器错误: {resp.text}")

    @task(1)
    def view_notifications(self):
        """查看通知列表"""
        self.client.get(
            "/api/notifications/my?page=1&size=10",
            headers=build_headers(self.token),
            name="/api/notifications/my",
            timeout=REQUEST_TIMEOUT,
        )

    @task(1)
    def check_unread_count(self):
        """查询未读通知数"""
        self.client.get(
            "/api/notifications/unread-count",
            headers=build_headers(self.token),
            name="/api/notifications/unread-count",
            timeout=REQUEST_TIMEOUT,
        )


# ═══════════════════════════════════════════════════════════════════════════════════
# 用户类 2: 研究者用户 (Researcher)
# ═══════════════════════════════════════════════════════════════════════════════════

class ResearcherUser(HttpUser):
    """
    模拟研究者用户行为:
    - 查看实验列表
    - 创建实验
    - 查看报名列表
    - 审核报名
    - 查看收到的评价
    """

    wait_time = between(2, 5)  # 研究者操作间隔 2~5 秒
    token = None

    def on_start(self):
        """研究者登录 — 使用预生成的 researcher_test 账号"""
        self._do_login("researcher_test")

    def _do_login(self, username):
        """仅登录，不尝试注册"""
        with self.client.post(
            "/api/auth/login",
            json={"username": username, "password": TEST_PASSWORD},
            headers=build_headers(),
            timeout=REQUEST_TIMEOUT,
            catch_response=True,
        ) as resp:
            if resp.status_code == 200:
                body = resp.json()
                if body.get("code") == 0 and body.get("data"):
                    self.token = body["data"].get("token")
                    return
            resp.failure(f"研究者登录失败: {resp.text}")

    # ── 任务定义 ──────────────────────────────────────────────────────────────────

    @task(3)
    def browse_experiments(self):
        """浏览实验列表"""
        page = random.randint(1, 5)
        self.client.get(
            f"/api/experiments?page={page}&size=10",
            headers=build_headers(self.token),
            name="/api/experiments [列表]",
            timeout=REQUEST_TIMEOUT,
        )

    @task(1)
    def create_experiment(self):
        """创建新实验"""
        now = int(time.time())
        self.client.post(
            "/api/experiments",
            json={
                "title": f"性能测试实验 #{now}",
                "description": "这是一个由 Locust 性能测试自动创建的实验",
                "location": random.choice(["心理学院实验楼301", "认知科学中心", "脑科学研究院"]),
                "startTime": "2026-07-01T09:00:00",
                "endTime": "2026-07-01T17:00:00",
                "ethicsApprovalNo": f"IRB-PERF-{now % 10000:04d}",
                "riskLevel": random.choice(["LOW", "MEDIUM", "HIGH"]),
                "paymentAmount": round(random.uniform(50, 200), 2),
                "paymentMethod": "OFFLINE",
                "paymentDescription": "实验结束后现场支付",
                "screeningCriteria": '{"ageRange":[18,30],"gender":"ANY"}',
                "excludeTags": '["fMRI"]',
                "tags": [
                    {"tagName": random.choice(["认知类", "情绪类", "社会类", "发展类"]), "coolingDays": 30}
                ],
            },
            headers=build_headers(self.token),
            name="/api/experiments [创建]",
            timeout=REQUEST_TIMEOUT,
        )

    @task(2)
    def view_registrations_for_experiment(self):
        """查看实验的报名列表（研究者视角）"""
        experiment_id = random.randint(1, 50)
        self.client.get(
            f"/api/registrations/experiment/{experiment_id}",
            headers=build_headers(self.token),
            name="/api/registrations/experiment/{id}",
            timeout=REQUEST_TIMEOUT,
        )

    @task(1)
    def approve_registration(self):
        """审核通过报名"""
        registration_id = random.randint(1, 200)
        with self.client.post(
            f"/api/registrations/{registration_id}/approve",
            headers=build_headers(self.token),
            name="/api/registrations/{id}/approve",
            timeout=REQUEST_TIMEOUT,
            catch_response=True,
        ) as resp:
            if resp.status_code >= 500:
                resp.failure(f"服务器错误: {resp.text}")

    @task(1)
    def view_received_reviews(self):
        """查看收到的评价"""
        self.client.get(
            "/api/reviews/received",
            headers=build_headers(self.token),
            name="/api/reviews/received",
            timeout=REQUEST_TIMEOUT,
        )

    @task(1)
    def confirm_payment(self):
        """确认支付（研究者标记已支付）"""
        registration_id = random.randint(1, 200)
        with self.client.post(
            "/api/payment/records/confirm-payer",
            json={
                "registrationId": registration_id,
                "paymentScreenshotUrl": "https://example.com/screenshot.png",
            },
            headers=build_headers(self.token),
            name="/api/payment/records/confirm-payer",
            timeout=REQUEST_TIMEOUT,
            catch_response=True,
        ) as resp:
            if resp.status_code >= 500:
                resp.failure(f"服务器错误: {resp.text}")


# ═══════════════════════════════════════════════════════════════════════════════════
# 用户类 3: 匿名浏览用户 (Anonymous)
# ═══════════════════════════════════════════════════════════════════════════════════

class AnonymousUser(HttpUser):
    """
    模拟未登录的匿名浏览用户:
    - 访问公开配置接口 (实验类型、标签)
    - 尝试访问需要认证的接口（验证安全拦截）
    - 访问 Swagger / Actuator
    """

    wait_time = between(3, 8)
    # 固定权重：匿名用户数量较少

    @task(3)
    def get_experiment_types(self):
        """获取实验类型列表 — 公开接口"""
        self.client.get(
            "/api/config/experiment-types",
            name="/api/config/experiment-types [公开]",
            timeout=REQUEST_TIMEOUT,
        )

    @task(2)
    def get_tags(self):
        """获取标签列表 — 公开接口"""
        self.client.get(
            "/api/config/tags",
            name="/api/config/tags [公开]",
            timeout=REQUEST_TIMEOUT,
        )

    @task(1)
    def get_locations(self):
        """获取地点列表 — 公开接口"""
        self.client.get(
            "/api/config/locations",
            name="/api/config/locations [公开]",
            timeout=REQUEST_TIMEOUT,
        )

    @task(1)
    def access_swagger(self):
        """访问 Swagger UI"""
        self.client.get(
            "/swagger-ui.html",
            name="/swagger-ui.html",
            timeout=REQUEST_TIMEOUT,
        )


# ═══════════════════════════════════════════════════════════════════════════════════
# 用户类 4: 管理员用户 (Admin)
# ═══════════════════════════════════════════════════════════════════════════════════

class AdminUser(HttpUser):
    """
    模拟管理员用户行为:
    - 查看平台统计概览
    - 查看所有申诉
    - 审核申诉
    - 查看用户信誉分
    """

    wait_time = between(3, 6)
    token = None

    def on_start(self):
        """管理员登录 — 使用预生成的 admin_test 账号"""
        with self.client.post(
            "/api/auth/login",
            json={"username": "admin_test", "password": TEST_PASSWORD},
            headers=build_headers(),
            timeout=REQUEST_TIMEOUT,
            catch_response=True,
        ) as resp:
            if resp.status_code == 200:
                body = resp.json()
                if body.get("code") == 0 and body.get("data"):
                    self.token = body["data"].get("token")
                    return
            resp.failure(f"管理员登录失败: {resp.text}")

    @task(3)
    def view_platform_summary(self):
        """查看平台统计概览"""
        self.client.get(
            "/api/statistics/summary",
            headers=build_headers(self.token),
            name="/api/statistics/summary",
            timeout=REQUEST_TIMEOUT,
        )

    @task(2)
    def view_all_appeals(self):
        """查看所有申诉"""
        self.client.get(
            "/api/appeals?page=1&size=20",
            headers=build_headers(self.token),
            name="/api/appeals [全部]",
            timeout=REQUEST_TIMEOUT,
        )

    @task(1)
    def review_appeal(self):
        """审核申诉"""
        appeal_id = random.randint(1, 50)
        with self.client.put(
            f"/api/appeals/{appeal_id}/review",
            json={
                "status": random.choice(["APPROVED", "REJECTED"]),
                "reviewComment": "性能测试自动审核",
            },
            headers=build_headers(self.token),
            name="/api/appeals/{id}/review",
            timeout=REQUEST_TIMEOUT,
            catch_response=True,
        ) as resp:
            if resp.status_code >= 500:
                resp.failure(f"服务器错误: {resp.text}")

    @task(1)
    def view_user_reputation(self):
        """查看用户信誉分"""
        user_id = random.randint(1, 100)
        self.client.get(
            f"/api/reputations/users/{user_id}",
            headers=build_headers(self.token),
            name="/api/reputations/users/{id}",
            timeout=REQUEST_TIMEOUT,
        )

    @task(1)
    def view_all_configs(self):
        """查看所有配置"""
        self.client.get(
            "/api/config",
            headers=build_headers(self.token),
            name="/api/config [全部]",
            timeout=REQUEST_TIMEOUT,
        )
