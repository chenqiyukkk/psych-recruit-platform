package com.project.user;

import java.util.Set;

public final class UserRoles {
  private UserRoles() {}

  public static final String SUBJECT = "被试";
  public static final String RESEARCHER = "研究者";
  public static final String ADMIN = "管理员";

  public static final Set<String> ALL = Set.of(SUBJECT, RESEARCHER, ADMIN);
}

