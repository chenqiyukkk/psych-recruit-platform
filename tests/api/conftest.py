import os
import uuid
from dataclasses import dataclass

import pytest
import requests


ROLE_SUBJECT = "被试"
ROLE_RESEARCHER = "研究者"
ROLE_ADMIN = "管理员"


def pytest_configure(config):
    config.addinivalue_line("markers", "integration: API integration tests that require the backend service")


def _env(name: str, default: str) -> str:
    value = os.getenv(name)
    return value if value else default


@dataclass
class ApiUser:
    username: str
    password: str
    role: str
    token: str | None = None
    user_id: int | None = None


class ApiClient:
    def __init__(self, base_url: str, timeout: int = 15):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()

    def url(self, path: str) -> str:
        return f"{self.base_url}{path}"

    def request(self, method: str, path: str, token: str | None = None, **kwargs):
        headers = dict(kwargs.pop("headers", {}) or {})
        if token:
            headers["Authorization"] = f"Bearer {token}"
        return self.session.request(
            method=method,
            url=self.url(path),
            headers=headers,
            timeout=self.timeout,
            **kwargs,
        )

    @staticmethod
    def json(response):
        return response.json()

    @staticmethod
    def assert_ok(response, expected_code: int = 0):
        assert response.status_code == 200, response.text
        payload = response.json()
        assert payload["code"] == expected_code, payload
        return payload["data"]

    @staticmethod
    def assert_api_error(response, expected_code: int, expected_http_status: int | None = None):
        if expected_http_status is not None:
            assert response.status_code == expected_http_status, response.text
        payload = response.json()
        assert payload["code"] == expected_code, payload
        return payload

    def unique_username(self, prefix: str) -> str:
        suffix = uuid.uuid4().hex[:8]
        return f"{prefix}_{suffix}"

    def register_user(self, username: str, password: str, role: str, phone: str | None = None, email: str | None = None):
        body = {
            "username": username,
            "password": password,
            "role": role,
            "phone": phone or f"138{uuid.uuid4().hex[:8]}"[:11],
            "email": email or f"{username}@example.com",
        }
        response = self.request("POST", "/api/auth/register", json=body)
        self.assert_ok(response)
        return body

    def login_user(self, username: str, password: str):
        response = self.request("POST", "/api/auth/login", json={"username": username, "password": password})
        data = self.assert_ok(response)
        token = data["token"]
        assert token
        return token

    def get_profile(self, token: str):
        response = self.request("GET", "/api/users/profile", token=token)
        return self.assert_ok(response)

    def update_profile(self, token: str, phone: str | None = None, email: str | None = None):
        body = {}
        if phone is not None:
            body["phone"] = phone
        if email is not None:
            body["email"] = email
        response = self.request("PUT", "/api/users/profile", token=token, json=body)
        return self.assert_ok(response)

    def create_experiment(self, token: str, title: str, payment_amount: str = "80.00", status: str | None = None):
        body = {
            "title": title,
            "description": f"{title} 描述",
            "location": "心理学院 101 室",
            "startTime": "2026-06-10T09:00:00",
            "endTime": "2026-06-10T11:00:00",
            "ethicsApprovalNo": f"IRB-{uuid.uuid4().hex[:8].upper()}",
            "riskLevel": "LOW",
            "paymentAmount": float(payment_amount),
            "paymentMethod": "OFFLINE",
            "paymentDescription": "实验结束后现场支付",
            "screeningCriteria": '{"ageRange":[18,25],"gender":"不限"}',
            "excludeTags": '["fMRI"]',
            "tags": [{"tagName": "认知类", "coolingDays": 30}],
        }
        response = self.request("POST", "/api/experiments", token=token, json=body)
        data = self.assert_ok(response)
        if status:
            assert data["status"] == status
        return data

    def publish_experiment(self, token: str, experiment_id: int):
        response = self.request("POST", f"/api/experiments/{experiment_id}/publish", token=token)
        self.assert_ok(response)

    def get_experiment(self, experiment_id: int):
        response = self.request("GET", f"/api/experiments/{experiment_id}")
        return self.assert_ok(response)

    def query_experiments(self, keyword: str | None = None, status: str | None = None, page: int = 0, size: int = 10):
        params = {"page": page, "size": size}
        if keyword is not None:
            params["keyword"] = keyword
        if status is not None:
            params["status"] = status
        response = self.request("GET", "/api/experiments", params=params)
        return self.assert_ok(response)

    def apply_registration(self, token: str, experiment_id: int):
        response = self.request("POST", f"/api/registrations/experiments/{experiment_id}", token=token)
        return self.assert_ok(response)

    def my_registrations(self, token: str):
        response = self.request("GET", "/api/registrations/my", token=token)
        return self.assert_ok(response)

    def approve_registration(self, token: str, registration_id: int):
        response = self.request("POST", f"/api/registrations/{registration_id}/approve", token=token)
        return self.assert_ok(response)

    def reject_registration(self, token: str, registration_id: int):
        response = self.request("POST", f"/api/registrations/{registration_id}/reject", token=token)
        return self.assert_ok(response)

    def sign_in(self, token: str, registration_id: int):
        response = self.request("POST", f"/api/sign-ins/registrations/{registration_id}", token=token)
        return self.assert_ok(response)

    def complete_experiment(self, token: str, registration_id: int):
        response = self.request("POST", f"/api/sign-ins/registrations/{registration_id}/complete", token=token)
        return self.assert_ok(response)

    def upload_payment_code(self, token: str, payment_type: str, qr_code_url: str, is_default: bool):
        body = {"paymentType": payment_type, "qrCodeUrl": qr_code_url, "isDefault": is_default}
        response = self.request("POST", "/api/payment/codes", token=token, json=body)
        return self.assert_ok(response)

    def my_payment_codes(self, token: str):
        response = self.request("GET", "/api/payment/codes/my", token=token)
        return self.assert_ok(response)

    def confirm_payer(self, token: str, registration_id: int, payee_user_id: int, amount: str, screenshot_url: str):
        body = {
            "registrationId": registration_id,
            "payeeUserId": payee_user_id,
            "amount": float(amount),
            "paymentScreenshotUrl": screenshot_url,
        }
        response = self.request("POST", "/api/payment/records/confirm-payer", token=token, json=body)
        return self.assert_ok(response)

    def confirm_payee(self, token: str, registration_id: int):
        response = self.request("POST", "/api/payment/records/confirm-payee", token=token, json={"registrationId": registration_id})
        return self.assert_ok(response)

    def dispute_payment(self, token: str, registration_id: int):
        response = self.request("POST", "/api/payment/records/dispute", token=token, json={"registrationId": registration_id})
        return self.assert_ok(response)

    def get_payment_record(self, token: str, registration_id: int):
        response = self.request("GET", f"/api/payment/records/{registration_id}", token=token)
        return self.assert_ok(response)

    def submit_review(self, token: str, registration_id: int, review_type: str, rating: int, comment: str, anonymous: bool = False):
        body = {
            "reviewType": review_type,
            "rating": rating,
            "communicationScore": rating,
            "professionalismScore": rating,
            "punctualityScore": rating,
            "comment": comment,
            "isAnonymous": anonymous,
        }
        response = self.request("POST", f"/api/reviews/registrations/{registration_id}", token=token, json=body)
        return self.assert_ok(response)

    def my_reviews(self, token: str):
        response = self.request("GET", "/api/reviews/my", token=token)
        return self.assert_ok(response)

    def received_reviews(self, token: str):
        response = self.request("GET", "/api/reviews/received", token=token)
        return self.assert_ok(response)

    def create_appeal(self, token: str, appeal_type: str, target_id: int, reason: str, evidence_urls: str | None = None):
        body = {
            "appealType": appeal_type,
            "targetId": target_id,
            "reason": reason,
            "evidenceUrls": evidence_urls or '["https://example.com/evidence.png"]',
        }
        response = self.request("POST", "/api/appeals", token=token, json=body)
        return self.assert_ok(response)

    def my_appeals(self, token: str):
        response = self.request("GET", "/api/appeals/my", token=token)
        return self.assert_ok(response)

    def review_appeal(self, token: str, appeal_id: int, decision: str, comment: str):
        body = {"decision": decision, "reviewComment": comment}
        response = self.request("PUT", f"/api/appeals/{appeal_id}/review", token=token, json=body)
        return self.assert_ok(response)


@pytest.fixture(scope="session")
def api_base_url():
    return _env("API_BASE_URL", "http://localhost:8080")


@pytest.fixture(scope="session")
def api_client(api_base_url):
    return ApiClient(api_base_url)


@pytest.fixture
def fresh_password():
    return "Test123456!"


@pytest.fixture
def subject_user(api_client, fresh_password):
    username = api_client.unique_username("subject")
    api_client.register_user(username, fresh_password, ROLE_SUBJECT)
    token = api_client.login_user(username, fresh_password)
    return ApiUser(username=username, password=fresh_password, role=ROLE_SUBJECT, token=token)


@pytest.fixture
def researcher_user(api_client, fresh_password):
    username = api_client.unique_username("researcher")
    api_client.register_user(username, fresh_password, ROLE_RESEARCHER)
    token = api_client.login_user(username, fresh_password)
    return ApiUser(username=username, password=fresh_password, role=ROLE_RESEARCHER, token=token)


@pytest.fixture
def admin_user(api_client, fresh_password):
    username = api_client.unique_username("admin")
    api_client.register_user(username, fresh_password, ROLE_ADMIN)
    token = api_client.login_user(username, fresh_password)
    return ApiUser(username=username, password=fresh_password, role=ROLE_ADMIN, token=token)


@pytest.fixture
def published_experiment(api_client, researcher_user):
    experiment = api_client.create_experiment(researcher_user.token, f"自动化测试实验-{uuid.uuid4().hex[:8]}")
    api_client.publish_experiment(researcher_user.token, experiment["id"])
    return api_client.get_experiment(experiment["id"])
