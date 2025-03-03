package com.stratocloud.provider.aliyun.common;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AliyunTimeUtil {

    public static LocalDateTime toLocalDateTime(String aliyunTime){
        return LocalDateTime.parse(aliyunTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"));
    }

    public static String toAliyunDateTime(LocalDateTime dateTime){
        return dateTime.atZone(
                ZoneId.systemDefault()
        ).withZoneSameInstant(
                ZoneId.of("UTC")
        ).format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        );
    }
}
