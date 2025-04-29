package com.stratocloud.utils;

import org.springframework.core.GenericTypeResolver;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Utils {
    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    public static int length(final Object[] arr) {
        return arr == null ? 0 : arr.length;
    }

    public static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(final CharSequence cs){
        return !isBlank(cs);
    }

    public static boolean isEmpty(final Collection<?> collection){
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(final Collection<?> collection){
        return !isEmpty(collection);
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return map!=null && !map.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(T o){
        return (Class<T>) o.getClass();
    }

    public static boolean isNotEmpty(Object[] array) {
        return array!=null && array.length>0;
    }

    public static boolean isNotEmpty(int[] array) {
        return array!=null && array.length>0;
    }

    public static boolean isEmpty(Object[] array) {
        return !isNotEmpty(array);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return !isNotEmpty(map);
    }

    public static <C> Class<?> getTypeArgumentClass(Class<? extends C> actualClass, Class<C> superClass){
        return GenericTypeResolver.resolveTypeArgument(actualClass, superClass);
    }

    public static int length(List<?> list) {
        if(isEmpty(list))
            return 0;
        return list.size();
    }

    public static String dropMiddleChars(String s, int keepNumber){
        if(s==null || s.length()<=keepNumber)
            return s;

        int half = keepNumber/2;
        return s.substring(0, half) + " ...(Dropped)... " + s.substring(s.length()-half);
    }

    public static boolean equalsIgnoreCase(String s1, String s2) {
        if(s1 == null || s2 == null)
            return false;

        return s1.equalsIgnoreCase(s2);
    }
}
