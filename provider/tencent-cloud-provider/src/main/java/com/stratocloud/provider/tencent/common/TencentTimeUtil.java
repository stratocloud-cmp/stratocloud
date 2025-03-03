package com.stratocloud.provider.tencent.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TencentTimeUtil {
    public static LocalDateTime toLocalDateTime(String tencentTime){
        return LocalDateTime.parse(tencentTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
