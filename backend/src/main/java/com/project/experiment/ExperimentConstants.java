package com.project.experiment;

import java.util.Set;

public final class ExperimentConstants {
  private ExperimentConstants() {}

  public static final String RISK_LOW = "LOW";
  public static final String RISK_MEDIUM = "MEDIUM";
  public static final String RISK_HIGH = "HIGH";
  public static final Set<String> RISK_LEVELS = Set.of(RISK_LOW, RISK_MEDIUM, RISK_HIGH);

  public static final String PAYMENT_OFFLINE = "OFFLINE";
  public static final String PAYMENT_ONLINE = "ONLINE";
  public static final Set<String> PAYMENT_METHODS = Set.of(PAYMENT_OFFLINE, PAYMENT_ONLINE);

  public static final String STATUS_DRAFT = "DRAFT";
  public static final String STATUS_PUBLISHED = "PUBLISHED";
  public static final String STATUS_RECRUITING = "RECRUITING";
  public static final String STATUS_FULL = "FULL";
  public static final String STATUS_ONGOING = "ONGOING";
  public static final String STATUS_COMPLETED = "COMPLETED";
  public static final Set<String> STATUSES =
      Set.of(
          STATUS_DRAFT,
          STATUS_PUBLISHED,
          STATUS_RECRUITING,
          STATUS_FULL,
          STATUS_ONGOING,
          STATUS_COMPLETED);
}

