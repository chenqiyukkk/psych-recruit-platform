package com.project.review;

import java.util.Set;

public final class ReviewConstants {


    private ReviewConstants(){}

    public static final String SUBJECT_TO_RESEARCHER = "SUBJECT_TO_RESEARCHER";
    public static final String RESEARCHER_TO_SUBJECT = "RESEARCHER_TO_SUBJECT";

    public static final Set<String> REVIEW_TYPES =
            Set.of(SUBJECT_TO_RESEARCHER,RESEARCHER_TO_SUBJECT);


}
