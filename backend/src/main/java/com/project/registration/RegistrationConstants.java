package com.project.registration;

import java.util.Set;

public final class RegistrationConstants {

    private RegistrationConstants(){

    }

    public static final String STATUS_PENDING = "PENDING";

    public static final String STATUS_APPROVED = "APPROVED";

    public static final String STATUS_REJECTED = "REJECTED";

    public static final String STATUS_CANCELLED = "CANCELLED";


    public static final Set<String> STATUSES =
            Set.of(STATUS_APPROVED,STATUS_CANCELLED,STATUS_PENDING,STATUS_REJECTED);
}
