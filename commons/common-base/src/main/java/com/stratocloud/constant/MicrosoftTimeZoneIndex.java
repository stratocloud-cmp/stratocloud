package com.stratocloud.constant;

import java.util.TimeZone;

public class MicrosoftTimeZoneIndex {

    //Greenwich Standard Time	(GMT) Casablanca, Monrovia
    public static final int GREENWICH_STANDARD_TIME_INDEX = 90;

    //China Standard Time	(GMT+08:00) Beijing, Chongqing, Hong Kong SAR, Urumqi
    public static final int CHINA_STANDARD_TIME_INDEX = 210;


    public static int getDefault(){
        String timeZoneId = TimeZone.getDefault().getID();

        if(timeZoneId.equalsIgnoreCase("Asia/Shanghai"))
            return CHINA_STANDARD_TIME_INDEX;

        return GREENWICH_STANDARD_TIME_INDEX;
    }
}
