package com.stratocloud.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TimeUtil {
    public static final DateTimeFormatter standardFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final ZoneId BEIJING_ZONE_ID = ZoneId.of("Asia/Shanghai");

    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    public static LocalDateTime fromUtcEpochMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(UTC_ZONE_ID).withZoneSameInstant(
                ZoneId.systemDefault()
        ).toLocalDateTime();
    }

    public static LocalDateTime fromString(String s){
        return LocalDateTime.parse(s, standardFormatter);
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(BEIJING_ZONE_ID).toInstant());
    }

    public static LocalDateTime fromDate(Date date) {
        return date.toInstant().atZone(BEIJING_ZONE_ID).toLocalDateTime();
    }

    public static LocalDateTime fromBeijingEpochMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(BEIJING_ZONE_ID).withZoneSameInstant(
                ZoneId.systemDefault()
        ).toLocalDateTime();
    }

    public static String toUtcTime(String localTime) {
        return LocalTime.parse(localTime).atDate(
                LocalDate.now()
        ).atZone(
                ZoneId.systemDefault()
        ).withZoneSameInstant(
                UTC_ZONE_ID
        ).toLocalDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
