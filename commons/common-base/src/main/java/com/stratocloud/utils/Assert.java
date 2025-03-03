package com.stratocloud.utils;

import java.util.Collection;

public class Assert {
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isFalse(boolean expression, String message) {
        isTrue(!expression, message);
    }

    public static void isNotEmpty(Object[] arr){
        isTrue(Utils.isNotEmpty(arr), "数组不能为空");
    }

    public static void isNotEmpty(Collection<?> collection){
        isNotEmpty(collection, "参数不能为空");
    }

    public static void isNotEmpty(Collection<?> collection, String message){
        isTrue(Utils.isNotEmpty(collection), message);
    }

    public static void isNotNull(Object o){
        isNotNull(o, "参数不能为空");
    }

    public static void isNotNull(Object o, String message){
        isTrue(o!=null, message);
    }

    public static void isNotBlank(String s){
        isTrue(Utils.isNotBlank(s), "参数不能为空");
    }


    public static void nonBlank(String... values) {
        if(Utils.isEmpty(values))
            return;

        for (String value : values) {
            isNotBlank(value);
        }
    }

    public static void isPositive(int... values) {
        if(values == null || values.length == 0)
            return;

        for (int value : values) {
            isTrue(value > 0, "Positive values required.");
        }
    }

    public static void isNotBlank(String value, String message) {
        isTrue(Utils.isNotBlank(value), message);
    }
}
