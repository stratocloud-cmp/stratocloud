package com.stratocloud.utils;

import io.micrometer.common.util.StringUtils;

public class EnvUtil {
    public static String getEnv(String name, String defaultValue){
        String result = System.getenv(name);
        return StringUtils.isBlank(result) ? defaultValue : result;
    }
}
