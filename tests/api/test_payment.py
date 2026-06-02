import pytest


pytestmark = pytest.mark.integration


def _prepare_paid_registration(api_client, researcher_user, subject_user):
    experiment = api_client.create_experiment(researcher_user.token, f"支付自动化-{api_client.unique_username('pay')}")
    api_client.publish_experiment(researcher_user.token, experiment["id"])
    applied = api_client.apply_registration(subject_user.token, experiment["id"])
    api_client.approve_registration(researcher_user.token, applied["id"])
    api_client.sign_in(researcher_user.token, applied["id"])
    api_client.complete_experiment(researcher_user.token, applied["id"])
    return applied


def test_upload_payment_code_and_confirm_payment(api_client, researcher_user, subject_user):
    registration = _prepare_paid_registration(api_client, researcher_user, subject_user)

    first_code = api_client.upload_payment_code(subject_user.token, "WECHAT", "https://example.com/wechat.png", True)
    assert first_code["paymentType"] == "WECHAT"
    assert first_code["isDefault"] is True

    second_code = api_client.upload_payment_code(subject_user.token, "ALIPAY", "https://example.com/alipay.png", True)
    assert second_code["paymentType"] == "ALIPAY"
    assert second_code["isDefault"] is True

    codes = api_client.my_payment_codes(subject_user.token)
    assert len(codes) == 2
    assert codes[0]["paymentType"] == "ALIPAY"
    assert codes[0]["isDefault"] is True
    assert codes[1]["paymentType"] == "WECHAT"
    assert codes[1]["isDefault"] is False

    payer_confirmed = api_client.confirm_payer(
        researcher_user.token,
        registration["id"],
        subject_user.user_id or api_client.get_profile(subject_user.token)["id"],
        80.0,
        "https://example.com/payment-proof.png",
    )
    assert payer_confirmed["status"] == "PAID"

    record_before = api_client.get_payment_record(subject_user.token, registration["id"])
    assert record_before["status"] == "PAID"

    payee_confirmed = api_client.confirm_payee(subject_user.token, registration["id"])
    assert payee_confirmed["status"] == "CONFIRMED"


def test_payment_dispute_flow(api_client, researcher_user, subject_user):
    registration = _prepare_paid_registration(api_client, researcher_user, subject_user)
    api_client.confirm_payer(
        researcher_user.token,
        registration["id"],
        api_client.get_profile(subject_user.token)["id"],
        80.0,
        "https://example.com/payment-proof-dispute.png",
    )

    disputed = api_client.dispute_payment(subject_user.token, registration["id"])
    assert disputed["status"] == "DISPUTED"
