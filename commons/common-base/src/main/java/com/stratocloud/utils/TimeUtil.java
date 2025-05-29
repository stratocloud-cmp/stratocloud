package com.stratocloud.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimeUtil {
    public static final ZoneId BEIJING_ZONE_ID = ZoneId.of("Asia/Shanghai");

    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    public static LocalDateTime fromUtcEpochMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(UTC_ZONE_ID).withZoneSameInstant(
                ZoneId.systemDefault()
        ).toLocalDateTime();
    }
}
