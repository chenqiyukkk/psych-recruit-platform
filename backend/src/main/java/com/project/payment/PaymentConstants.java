package com.project.payment;

import java.util.Set;

public final class PaymentConstants {
  private PaymentConstants() {}

  public static final String CODE_WECHAT = "WECHAT";
  public static final String CODE_ALIPAY = "ALIPAY";
  public static final Set<String> CODE_TYPES = Set.of(CODE_WECHAT, CODE_ALIPAY);

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_PAID = "PAID";
  public static final String STATUS_CONFIRMED = "CONFIRMED";
  public static final String STATUS_DISPUTED = "DISPUTED";
  public static final Set<String> STATUSES =
      Set.of(STATUS_PENDING, STATUS_PAID, STATUS_CONFIRMED, STATUS_DISPUTED);
}

