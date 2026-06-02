import pytest


pytestmark = pytest.mark.integration


def _prepare_reviewable_registration(api_client, researcher_user, subject_user):
    experiment = api_client.create_experiment(researcher_user.token, f"评价自动化-{api_client.unique_username('rev')}")
    api_client.publish_experiment(researcher_user.token, experiment["id"])
    applied = api_client.apply_registration(subject_user.token, experiment["id"])
    api_client.approve_registration(researcher_user.token, applied["id"])
    api_client.sign_in(researcher_user.token, applied["id"])
    api_client.complete_experiment(researcher_user.token, applied["id"])
    return applied, experiment


def test_bidirectional_review_and_duplicate_rejection(api_client, researcher_user, subject_user):
    registration, _ = _prepare_reviewable_registration(api_client, researcher_user, subject_user)

    subject_review = api_client.submit_review(
        subject_user.token,
        registration["id"],
        "SUBJECT_TO_RESEARCHER",
        5,
        "研究者沟通顺畅，实验安排清晰",
        True,
    )
    assert subject_review["reviewType"] == "SUBJECT_TO_RESEARCHER"
    assert subject_review["isAnonymous"] is True

    researcher_review = api_client.submit_review(
        researcher_user.token,
        registration["id"],
        "RESEARCHER_TO_SUBJECT",
        4,
        "被试配合度高，签到准时",
        False,
    )
    assert researcher_review["reviewType"] == "RESEARCHER_TO_SUBJECT"

    my_reviews = api_client.my_reviews(subject_user.token)
    received_reviews = api_client.received_reviews(researcher_user.token)
    assert len(my_reviews) == 1
    assert len(received_reviews) == 1

    duplicate = api_client.request(
        "POST",
        f"/api/reviews/registrations/{registration['id']}",
        token=subject_user.token,
        json={
            "reviewType": "SUBJECT_TO_RESEARCHER",
            "rating": 4,
            "communicationScore": 4,
            "professionalismScore": 4,
            "punctualityScore": 4,
            "comment": "重复评价",
            "isAnonymous": True,
        },
    )
    api_client.assert_api_error(duplicate, expected_code=400, expected_http_status=400)


def test_appeal_flow_for_review(api_client, researcher_user, subject_user, admin_user):
    registration, _ = _prepare_reviewable_registration(api_client, researcher_user, subject_user)
    review = api_client.submit_review(
        subject_user.token,
        registration["id"],
        "SUBJECT_TO_RESEARCHER",
        2,
        "评分偏低，申请申诉",
        False,
    )

    appeal = api_client.create_appeal(subject_user.token, "LOW_RATING", review["id"], "评分与实际体验不符")
    assert appeal["status"] == "PENDING"

    reviewed = api_client.review_appeal(admin_user.token, appeal["id"], "APPROVED", "证据充分，同意处理")
    assert reviewed["status"] == "APPROVED"
