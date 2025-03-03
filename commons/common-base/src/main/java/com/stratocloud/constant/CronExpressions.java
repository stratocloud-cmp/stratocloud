package com.stratocloud.constant;

public class CronExpressions {
    public static final String EVERY_DAY_MIDNIGHT = "0 0 0 * * *";
    public static final String EVERY_DAY_2_AM = "0 0 2 * * *";
    public static final String EVERY_TEN_SECONDS = "*/10 * * * * ?";

    public static final String EVERY_TEN_MINUTES = "0 0/10 * * * ?";

    public static final String EVERY_THIRTY_MINUTES = "0 0/30 * * * ?";

    public static final String EVERY_HOUR = "0 0 * * * ?";

}
