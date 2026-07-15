package com.project.reputation;

public  final class ReputationConstants {

    private ReputationConstants(){}

    public static final String CHANGE_TYPE_ADMIN_ADJUST = "ADMIN_ADJUST";
    public static final String CHANGE_TYPE_COMPLETED = "COMPLETED";
    public static final String CHANGE_TYPE_NO_SHOW = "NO_SHOW";

    public static final int COMPLETED_BONUS = 2;
    public static final int NO_SHOW_PENALTY = -20;

    public static final int MIN_SCORE = 0;

    public static final int MAX_SCORE = 100;
}
