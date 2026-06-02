import pytest


pytestmark = pytest.mark.integration


def test_register_login_and_profile_roundtrip(api_client, fresh_password):
    username = api_client.unique_username("auth_subject")

    api_client.register_user(username, fresh_password, "被试", phone="13800001111", email=f"{username}@example.com")
    token = api_client.login_user(username, fresh_password)

    profile = api_client.get_profile(token)
    assert profile["username"] == username
    assert profile["role"] == "被试"
    assert profile["reputationScore"] == 100

    updated = api_client.update_profile(token, phone="13900002222", email=f"{username}.new@example.com")
    assert updated["phone"] == "13900002222"
    assert updated["email"] == f"{username}.new@example.com"


def test_login_fails_with_wrong_password(api_client, fresh_password):
    username = api_client.unique_username("auth_fail")
    api_client.register_user(username, fresh_password, "被试")

    response = api_client.request("POST", "/api/auth/login", json={"username": username, "password": "wrong-password"})
    payload = api_client.assert_api_error(response, expected_code=401, expected_http_status=400)
    assert "密码" in payload["message"] or "登录" in payload["message"]
