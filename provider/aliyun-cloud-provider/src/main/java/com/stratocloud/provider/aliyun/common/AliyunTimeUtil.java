package com.stratocloud.provider.aliyun.common;

import com.stratocloud.utils.TimeUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AliyunTimeUtil {

    public static LocalDateTime toLocalDateMinutesTime(String aliyunTime){
        return LocalDateTime.parse(aliyunTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'")).atZone(
                TimeUtil.UTC_ZONE_ID
        ).withZoneSameInstant(
                ZoneId.systemDefault()
        ).toLocalDateTime();
    }

    public static LocalDateTime toLocalDateSecondsTime(String aliyunTime){
        return LocalDateTime.parse(aliyunTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")).atZone(
                TimeUtil.UTC_ZONE_ID
        ).withZoneSameInstant(
                ZoneId.systemDefault()
        ).toLocalDateTime();
    }

    public static String toAliyunDateTime(LocalDateTime dateTime){
        return dateTime.atZone(
                ZoneId.systemDefault()
        ).withZoneSameInstant(
                TimeUtil.UTC_ZONE_ID
        ).format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        );
    }
}
