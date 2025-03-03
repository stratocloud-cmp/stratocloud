package com.stratocloud.utils;

public class CompareUtil {
    public static int compareBoolean(Boolean b1, Boolean b2) {
        if(b1==null && b2==null)
            return 0;

        if(b1==null)
            return -1;

        if(b2==null)
            return 1;

        return b1.compareTo(b2);
    }

    public static int compareBooleanDesc(Boolean b1, Boolean b2){
        return -compareBoolean(b1, b2);
    }
}
