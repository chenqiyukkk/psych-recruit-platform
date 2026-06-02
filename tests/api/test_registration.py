import pytest


pytestmark = pytest.mark.integration


def test_apply_approve_and_duplicate_registration(api_client, researcher_user, subject_user):
    experiment = api_client.create_experiment(researcher_user.token, f"报名自动化-{api_client.unique_username('reg')}")
    api_client.publish_experiment(researcher_user.token, experiment["id"])

    applied = api_client.apply_registration(subject_user.token, experiment["id"])
    assert applied["status"] == "PENDING"

    my_regs = api_client.my_registrations(subject_user.token)
    assert any(item["id"] == applied["id"] for item in my_regs)

    approved = api_client.approve_registration(researcher_user.token, applied["id"])
    assert approved["status"] == "APPROVED"

    duplicate = api_client.request("POST", f"/api/registrations/experiments/{experiment['id']}", token=subject_user.token)
    api_client.assert_api_error(duplicate, expected_code=400, expected_http_status=400)


def test_reject_registration(api_client, researcher_user, subject_user):
    experiment = api_client.create_experiment(researcher_user.token, f"拒绝报名-{api_client.unique_username('rej')}")
    api_client.publish_experiment(researcher_user.token, experiment["id"])

    applied = api_client.apply_registration(subject_user.token, experiment["id"])
    rejected = api_client.reject_registration(researcher_user.token, applied["id"])
    assert rejected["status"] == "REJECTED"
