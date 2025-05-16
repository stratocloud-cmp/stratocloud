package com.stratocloud.provider.tencent.common;

import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TencentTimeUtil {
    public static LocalDateTime toLocalDateTime(String tencentTime){
        return LocalDateTime.parse(tencentTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static String fromLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime.truncatedTo(ChronoUnit.SECONDS).atZone(
                ZoneId.systemDefault()
        ).format(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
        );
    }

    public static LocalDateTime fromEpochSeconds(String epochSeconds) {
        if(Utils.isBlank(epochSeconds))
            return LocalDateTime.now();

        return LocalDateTime.ofEpochSecond(
                Long.parseLong(epochSeconds),
                0,
                TimeUtil.BEIJING_ZONE_ID.getRules().getOffset(LocalDateTime.now())
        );
    }
}
