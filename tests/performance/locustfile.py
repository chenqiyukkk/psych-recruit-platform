"""
心理学被试招募平台 — 性能测试脚本 (Locust)

用法:
    locust -f tests/performance/locustfile.py --host=http://localhost:8080

    # 无 UI 模式
    locust -f tests/performance/locustfile.py --host=http://localhost:8080 \
        --headless --users 100 --spawn-rate 10 --run-time 300s \
        --html=report.html --csv=results

前置条件:
    1. 后端已启动: cd backend && mvn spring-boot:run
    2. 已生成测试数据: python scripts/generate_test_data.py
    3. 依赖已安装: pip install locust
"""

import random
from locust import HttpUser, task, between, events
from locust.runners import MasterRunner

# ═══════════════════════════════════════════════════════════════════════════════════
# 配置
# ═══════════════════════════════════════════════════════════════════════════════════
TEST_PASSWORD = "123456"
REQUEST_TIMEOUT = 10

# 预生成的测试账号（由 scripts/generate_test_data.py 创建）
TEST_ACCOUNTS = {
    "subject": ["subject_test"],
    "researcher": ["researcher_test"],
    "admin": ["admin_test"],
}


def auth_headers(token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


# ═══════════════════════════════════════════════════════════════════════════════════
# 生命周期事件
# ═══════════════════════════════════════════════════════════════════════════════════

@events.init.add_listener
def on_locust_init(environment, **kwargs):
    if isinstance(environment.runner, MasterRunner):
        print(f"📍 性能测试 - 目标: {environment.host}")


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    s = environment.stats
    if s.total.num_requests == 0:
        return
    avg = s.total.avg_response_time
    p50 = s.total.get_response_time_percentile(0.5)
    p95 = s.total.get_response_time_percentile(0.95)
    p99 = s.total.get_response_time_percentile(0.99)
    rps = s.total.total_rps
    fr = s.total.fail_ratio * 100

    print("\n" + "=" * 60)
    print("📊 性能测试结果")
    print("=" * 60)
    print(f"  总请求:       {s.total.num_requests}")
    print(f"  失败:         {s.total.num_failures}  ({fr:.1f}%)")
    print(f"  平均响应:     {avg:.0f} ms")
    print(f"  P50:          {p50:.0f} ms")
    print(f"  P95:          {p95:.0f} ms")
    print(f"  P99:          {p99:.0f} ms")
    print(f"  最大:         {s.total.max_response_time:.0f} ms")
    print(f"  RPS:          {rps:.1f}")
    print("=" * 60)
    print(f"  {'✅' if avg < 500 else '❌'} 平均响应 < 500ms  ({avg:.0f}ms)")
    print(f"  {'✅' if p95 < 2000 else '❌'} P95 < 2s  ({p95:.0f}ms)")
    print(f"  {'✅' if fr < 5 else '❌'} 失败率 < 5%  ({fr:.1f}%)")
    print("=" * 60 + "\n")


# ═══════════════════════════════════════════════════════════════════════════════════
# 请求辅助方法 — 统一处理 4xx/5xx
# ═══════════════════════════════════════════════════════════════════════════════════

class BaseUser(HttpUser):
    """所有用户类的基类，提供统一的请求封装。"""
    abstract = True
    token = None

    def _get(self, path, name=None, **kw):
        """GET 请求：4xx=success, 5xx=failure"""
        return self.client.get(
            path,
            headers=auth_headers(self.token),
            timeout=REQUEST_TIMEOUT,
            name=name or path,
            catch_response=True,
            **kw,
        )

    def _post(self, path, name=None, json=None, **kw):
        """POST 请求：4xx=success, 5xx=failure"""
        return self.client.post(
            path,
            json=json,
            headers=auth_headers(self.token),
            timeout=REQUEST_TIMEOUT,
            name=name or path,
            catch_response=True,
            **kw,
        )

    def _put(self, path, name=None, json=None, **kw):
        """PUT 请求：4xx=success, 5xx=failure"""
        return self.client.put(
            path,
            json=json,
            headers=auth_headers(self.token),
            timeout=REQUEST_TIMEOUT,
            name=name or path,
            catch_response=True,
            **kw,
        )

    @staticmethod
    def _check(resp):
        """判定失败: 连接错误(status=0) 或 5xx。4xx 是业务拒绝，算成功。"""
        if not hasattr(resp, 'status_code') or resp.status_code == 0:
            resp.failure("Connection refused / timeout")
        elif resp.status_code >= 500:
            resp.failure(f"5xx: {resp.status_code}")
        else:
            resp.success()


def login(client, username):
    """登录并返回 token，失败返回 None"""
    with client.post(
        "/api/auth/login",
        json={"username": username, "password": TEST_PASSWORD},
        headers=auth_headers(),
        timeout=REQUEST_TIMEOUT,
        catch_response=True,
    ) as resp:
        if resp.status_code == 200:
            body = resp.json()
            if body.get("code") == 0 and body.get("data"):
                return body["data"].get("token")
        resp.success()  # 不因登录失败影响测试统计
    return None


# ═══════════════════════════════════════════════════════════════════════════════════
# 被试用户
# ═══════════════════════════════════════════════════════════════════════════════════

class SubjectUser(BaseUser):
    wait_time = between(1, 3)

    def on_start(self):
        self.token = login(self.client, "subject_test")

    @task(4)
    def browse_experiments(self):
        page = random.randint(1, 3)
        with self._get(f"/api/experiments?page={page}&size=10", name="/api/experiments [列表]") as resp:
            self._check(resp)

    @task(2)
    def view_experiment_detail(self):
        eid = random.randint(1, 50)
        with self._get(f"/api/experiments/{eid}", name="/api/experiments/{id} [详情]") as resp:
            self._check(resp)

    @task(1)
    def view_my_profile(self):
        with self._get("/api/users/profile") as resp:
            self._check(resp)

    @task(2)
    def view_my_registrations(self):
        with self._get("/api/registrations/my") as resp:
            self._check(resp)

    @task(1)
    def register_for_experiment(self):
        eid = random.randint(1, 50)
        with self._post(f"/api/registrations/experiments/{eid}", name="/api/registrations [报名]") as resp:
            self._check(resp)

    @task(1)
    def view_notifications(self):
        with self._get("/api/notifications/my?page=1&size=10") as resp:
            self._check(resp)

    @task(1)
    def check_unread_count(self):
        with self._get("/api/notifications/unread-count") as resp:
            self._check(resp)


# ═══════════════════════════════════════════════════════════════════════════════════
# 研究者用户
# ═══════════════════════════════════════════════════════════════════════════════════

class ResearcherUser(BaseUser):
    wait_time = between(2, 5)

    def on_start(self):
        self.token = login(self.client, "researcher_test")

    @task(3)
    def browse_experiments(self):
        with self._get("/api/experiments?page=1&size=10", name="/api/experiments [列表]") as resp:
            self._check(resp)

    @task(1)
    def create_experiment(self):
        ts = random.randint(1000, 9999)
        with self._post(
            "/api/experiments",
            json={
                "title": f"性能测试实验 #{ts}",
                "description": "Locust自动创建",
                "location": random.choice(["心理学院实验楼301", "认知科学中心"]),
                "startTime": "2026-07-01T09:00:00",
                "endTime": "2026-07-01T17:00:00",
                "ethicsApprovalNo": f"IRB-{ts}",
                "riskLevel": random.choice(["LOW", "MEDIUM"]),
                "paymentAmount": round(random.uniform(50, 200), 2),
                "paymentMethod": "OFFLINE",
                "paymentDescription": "实验结束后现场支付",
                "screeningCriteria": '{"ageRange":[18,30],"gender":"ANY"}',
                "excludeTags": '[]',
                "tags": [{"tagName": random.choice(["认知类", "情绪类", "社会类"]), "coolingDays": 30}],
            },
            name="/api/experiments [创建]",
        ) as resp:
            self._check(resp)

    @task(2)
    def view_registrations_for_experiment(self):
        eid = random.randint(1, 50)
        with self._get(f"/api/registrations/experiment/{eid}", name="/api/registrations/experiment/{id}") as resp:
            self._check(resp)

    @task(1)
    def approve_registration(self):
        rid = random.randint(1, 200)
        with self._post(f"/api/registrations/{rid}/approve", name="/api/registrations/{id}/approve") as resp:
            self._check(resp)

    @task(1)
    def view_received_reviews(self):
        with self._get("/api/reviews/received") as resp:
            self._check(resp)

    @task(1)
    def confirm_payment(self):
        rid = random.randint(1, 200)
        with self._post(
            "/api/payment/records/confirm-payer",
            json={
                "registrationId": rid,
                "payeeUserId": random.randint(1, 100),
                "amount": round(random.uniform(50, 200), 2),
                "paymentScreenshotUrl": "https://example.com/screenshot.png",
            },
            name="/api/payment/records/confirm-payer",
        ) as resp:
            self._check(resp)


# ═══════════════════════════════════════════════════════════════════════════════════
# 匿名用户
# ═══════════════════════════════════════════════════════════════════════════════════

class AnonymousUser(BaseUser):
    wait_time = between(3, 8)
    # 匿名用户无需登录

    @task(3)
    def get_experiment_types(self):
        with self._get("/api/config/experiment-types", name="/api/config/experiment-types [公开]") as resp:
            self._check(resp)

    @task(2)
    def get_tags(self):
        with self._get("/api/config/tags", name="/api/config/tags [公开]") as resp:
            self._check(resp)

    @task(1)
    def get_locations(self):
        with self._get("/api/config/locations", name="/api/config/locations [公开]") as resp:
            self._check(resp)

    @task(1)
    def access_swagger(self):
        with self._get("/swagger-ui.html") as resp:
            self._check(resp)


# ═══════════════════════════════════════════════════════════════════════════════════
# 管理员用户
# ═══════════════════════════════════════════════════════════════════════════════════

class AdminUser(BaseUser):
    wait_time = between(3, 6)

    def on_start(self):
        self.token = login(self.client, "admin_test")

    @task(3)
    def view_platform_summary(self):
        with self._get("/api/statistics/summary") as resp:
            self._check(resp)

    @task(2)
    def view_all_appeals(self):
        with self._get("/api/appeals?page=1&size=20", name="/api/appeals [全部]") as resp:
            self._check(resp)

    @task(1)
    def review_appeal(self):
        aid = random.randint(1, 50)
        with self._put(
            f"/api/appeals/{aid}/review",
            json={"decision": random.choice(["APPROVED", "REJECTED"]), "reviewComment": "自动审核"},
            name="/api/appeals/{id}/review",
        ) as resp:
            self._check(resp)

    @task(1)
    def view_user_reputation(self):
        uid = random.randint(1, 100)
        with self._get(f"/api/reputations/users/{uid}", name="/api/reputations/users/{id}") as resp:
            self._check(resp)

    @task(1)
    def view_all_configs(self):
        with self._get("/api/config", name="/api/config [全部]") as resp:
            self._check(resp)
