package com.stratocloud.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class TimeUtil {
    public static final ZoneId BEIJING_ZONE_ID = ZoneId.of("Asia/Shanghai");

    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    public static LocalDateTime fromUtcEpochMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(UTC_ZONE_ID).withZoneSameInstant(
                ZoneId.systemDefault()
        ).toLocalDateTime();
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(BEIJING_ZONE_ID).toInstant());
    }

    public static LocalDateTime fromDate(Date date) {
        return date.toInstant().atZone(BEIJING_ZONE_ID).toLocalDateTime();
    }
}
