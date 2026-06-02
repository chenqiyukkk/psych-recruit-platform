import pytest


pytestmark = pytest.mark.integration


def test_create_publish_query_and_update_experiment(api_client, researcher_user):
    title = f"实验管理自动化-{api_client.unique_username('exp')}"
    experiment = api_client.create_experiment(researcher_user.token, title)
    assert experiment["title"] == title
    assert experiment["status"] == "DRAFT"

    api_client.publish_experiment(researcher_user.token, experiment["id"])
    published = api_client.get_experiment(experiment["id"])
    assert published["status"] == "PUBLISHED"

    query = api_client.query_experiments(keyword=title, status="PUBLISHED", page=0, size=10)
    assert query["totalElements"] >= 1
    assert any(item["id"] == experiment["id"] for item in query["content"])

    response = api_client.request(
        "PUT",
        f"/api/experiments/{experiment['id']}",
        token=researcher_user.token,
        json={"description": "更新后的实验说明"},
    )
    updated = api_client.assert_ok(response)
    assert updated["description"] == "更新后的实验说明"


def test_subject_cannot_create_experiment(api_client, subject_user):
    response = api_client.request(
        "POST",
        "/api/experiments",
        token=subject_user.token,
        json={
            "title": "不允许创建",
            "description": "subject should not create",
            "location": "心理学院",
            "startTime": "2026-06-10T09:00:00",
            "endTime": "2026-06-10T11:00:00",
            "ethicsApprovalNo": "IRB-FAIL-001",
            "riskLevel": "LOW",
            "paymentAmount": "50.00",
            "paymentMethod": "OFFLINE",
        },
    )
    payload = api_client.assert_api_error(response, expected_code=403, expected_http_status=400)
    assert "研究者" in payload["message"]
